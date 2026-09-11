package com.actionth.membership.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.constant.WebhookLogType;
import com.actionth.membership.model.OrderRequestLog;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.PaymentReviewAudit;
import com.actionth.membership.model.PaymentWebhookLog;
import com.actionth.membership.model.dto.ResolveReviewResponse;
import com.actionth.membership.model.dto.ReviewContextDto;
import com.actionth.membership.model.dto.ReviewListItemDto;
import com.actionth.membership.model.dto.ReviewContextDto.FieldChange;
import com.actionth.membership.model.dto.ReviewContextDto.OfferedCombo;
import com.actionth.membership.model.dto.ReviewContextDto.OrderState;
import com.actionth.membership.model.dto.ReviewContextDto.ReceivedPayment;
import com.actionth.membership.model.dto.ReviewContextDto.Suggestion;
import com.actionth.membership.model.dto.ReviewContextDto.TimelineEvent;
import com.actionth.membership.model.request.ResolveReviewRequest;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.repository.OrderRequestLogRepository;
import com.actionth.membership.repository.PaymentReviewAuditRepository;
import com.actionth.membership.repository.PaymentWebhookLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReviewService {

    private static final double AMOUNT_TOLERANCE = 1.0;

    private final OrderRepository orderRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final OrderRequestLogRepository orderRequestLogRepository;
    private final PaymentReviewAuditRepository auditRepository;
    private final DistributedLockService distributedLockService;
    private final PaymentWebhookService paymentWebhookService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    public List<ReviewListItemDto> listReviews() {
        List<ReviewListItemDto> out = new ArrayList<>();
        for (Orders order : orderRepository.findByPaymentStatusOrderByCreatedTimeDesc(PaymentStatus.REVIEW.toString())) {
            out.add(ReviewListItemDto.builder()
                    .orderNo(order.getOrderNo())
                    .eventName(order.getEvent() != null ? order.getEvent().getName() : null)
                    .reviewReason(order.getReviewReason())
                    .paymentMethod(order.getPaymentMethod())
                    .totalAmountWithFee(order.getTotalAmountWithFee())
                    .paymentDueDatetime(order.getPaymentDueDatetime())
                    .createdTime(order.getCreatedTime())
                    .build());
        }
        return out;
    }

    public ReviewContextDto buildReviewContext(String orderNo) {
        Orders order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderNo));

        List<OfferedCombo> combos = parseOfferedCombos(orderNo);
        List<ReceivedPayment> received = buildReceivedPayments(order);
        List<ReceivedPayment> matched = successfulMatchedPayments(received, combos);

        Suggestion suggestion = null;
        List<FieldChange> expectedChanges = null;
        boolean noMatch = false;
        if (matched.size() == 1) {
            ReceivedPayment rp = matched.get(0);
            OfferedCombo combo = matchCombo(rp.getAmount(), rp.getResolvedMethod(), combos);
            suggestion = Suggestion.builder()
                    .transactionId(rp.getTransactionId())
                    .combo(combo)
                    .matched(true)
                    .build();
            expectedChanges = buildExpectedChanges(order, combo, rp.getTransactionId(), rp.getReceivedDateTime());
        } else if (matched.isEmpty()) {
            noMatch = true;
        }

        return ReviewContextDto.builder()
                .orderNo(orderNo)
                .currentState(toState(order))
                .offeredCombos(combos)
                .receivedPayments(received)
                .timeline(buildTimeline(order))
                .suggestion(suggestion)
                .expectedChanges(expectedChanges)
                .multipleSuccessfulPayments(matched.size() >= 2)
                .noMatch(noMatch)
                .build();
    }

    public ResolveReviewResponse resolveReview(String orderNo, ResolveReviewRequest request) {
        Integer adminId = currentAdminUserId();
        return distributedLockService.executeWithLock(
                PaymentWebhookService.ORDER_PAY_LOCK_PREFIX + orderNo,
                () -> doResolve(orderNo, request, adminId));
    }

    private ResolveReviewResponse doResolve(String orderNo, ResolveReviewRequest request, Integer adminId) {
        Orders order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderNo));

        if (!PaymentStatus.REVIEW.toString().equalsIgnoreCase(order.getPaymentStatus())) {
            writeAudit(orderNo, adminId, "RESOLVE_REVIEW", "NOT_IN_REVIEW",
                    request.getTransactionId(), toJson(toState(order)), null, request.getReason());
            return response(false, "NOT_IN_REVIEW",
                    "Order is not in REVIEW (current status: " + order.getPaymentStatus() + ")", orderNo, order);
        }

        List<OfferedCombo> combos = parseOfferedCombos(orderNo);
        List<ReceivedPayment> received = buildReceivedPayments(order);
        List<ReceivedPayment> matched = successfulMatchedPayments(received, combos);

        if (matched.size() >= 2 && !request.isConfirmDoublePay()) {
            writeAudit(orderNo, adminId, "RESOLVE_REVIEW", "DOUBLE_PAY_BLOCKED",
                    request.getTransactionId(), toJson(toState(order)), null, request.getReason());
            return response(false, "DOUBLE_PAY_BLOCKED",
                    "Possible double payment (" + matched.size() + " distinct successful payments) — manual refund "
                            + "review required. Set confirmDoublePay=true to settle the chosen transaction.",
                    orderNo, order);
        }

        PaymentWebhookLog paidLog = findLogForOrderTxn(order, request.getTransactionId());
        Double webhookAmount = paidLog != null ? paidLog.getAmount() : null;
        String resolvedMethod = paidLog != null
                ? paymentWebhookService.resolveMethodFromWebhookLog(paidLog.getPaymentProvider(), paidLog.getPayloadJson())
                : null;
        OffsetDateTime paidAt = paidLog != null ? paidLog.getReceivedDateTime() : null;

        boolean overrideProvided = notBlank(request.getOverrideMethod()) || request.getOverrideAmount() != null;
        String effectiveMethod = notBlank(request.getOverrideMethod()) ? request.getOverrideMethod() : resolvedMethod;
        Double effectiveAmount = request.getOverrideAmount() != null ? request.getOverrideAmount() : webhookAmount;

        if (effectiveAmount == null) {
            writeAudit(orderNo, adminId, "RESOLVE_REVIEW", "NO_MATCH",
                    request.getTransactionId(), toJson(toState(order)), null, request.getReason());
            return response(false, "NO_MATCH",
                    "No settlement amount: transactionId did not match a received payment and no override was given.",
                    orderNo, order);
        }

        OfferedCombo matchedTarget = matchCombo(effectiveAmount, effectiveMethod, combos);
        if (matchedTarget == null) {
            if (!overrideProvided) {
                writeAudit(orderNo, adminId, "RESOLVE_REVIEW", "NO_MATCH",
                        request.getTransactionId(), toJson(toState(order)), null, request.getReason());
                return response(false, "NO_MATCH",
                        "Paid amount " + effectiveAmount + " matches no single offered combo. Provide an override to force.",
                        orderNo, order);
            }
            matchedTarget = syntheticCombo(effectiveMethod, effectiveAmount, order.getTotalPrice());
        }

        final OfferedCombo target = matchedTarget;
        final String txnId = request.getTransactionId();
        final OffsetDateTime settleAt = paidAt != null ? paidAt : OffsetDateTime.now();
        final String beforeJson = toJson(toState(order));
        final String afterJson = toJson(OrderState.builder()
                .paymentStatus(PaymentStatus.SUCCESS.toString())
                .reviewReason(null)
                .paymentMethod(target.getPaymentMethod())
                .feePercent(target.getFeePercent())
                .fee(target.getFee())
                .totalAmountWithFee(target.getTotalAmountWithFee())
                .scbTransactionId(notBlank(txnId) ? txnId : order.getScbTransactionId())
                .paymentDateTime(settleAt)
                .build());

        new TransactionTemplate(transactionManager).execute(status -> {
            order.setPaymentMethod(target.getPaymentMethod());
            order.setFeePercent(target.getFeePercent());
            order.setFee(target.getFee());
            order.setTotalAmountWithFee(target.getTotalAmountWithFee());
            order.setPaymentStatus(PaymentStatus.SUCCESS.toString());
            order.setPaymentDateTime(settleAt);
            if (notBlank(txnId)) {
                order.setScbTransactionId(txnId);
            }
            order.setReviewReason(null);
            orderRepository.save(order);
            auditRepository.saveAndFlush(buildAudit(orderNo, adminId, "RESOLVE_REVIEW", "RESOLVED",
                    txnId, beforeJson, afterJson, request.getReason()));
            return null;
        });

        if (request.isSendEmail()) {
            try {
                Orders refreshed = orderRepository.findByOrderNo(orderNo).orElse(order);
                paymentWebhookService.sendSuccessEmail(refreshed, txnId);
            } catch (Exception e) {
                log.error("[resolveReview] sendSuccessEmail failed for {}: {}", orderNo, e.getMessage());
            }
        }

        return response(true, "RESOLVED",
                "Settled as " + target.getPaymentMethod() + " / " + target.getTotalAmountWithFee(), orderNo, order);
    }

    private List<OfferedCombo> parseOfferedCombos(String orderNo) {
        List<OfferedCombo> combos = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (OrderRequestLog reqLog : orderRequestLogRepository.findByOrderNoOrderByIdAsc(orderNo)) {
            if (reqLog.getRequestBody() == null) {
                continue;
            }
            try {
                Map<String, Object> body = objectMapper.readValue(reqLog.getRequestBody(),
                        new TypeReference<Map<String, Object>>() {});
                String method = str(body.get("paymentMethod"));
                Double total = dbl(body.get("totalAmountWithFee"));
                if (method == null || method.isBlank() || total == null) {
                    continue;
                }
                OfferedCombo combo = OfferedCombo.builder()
                        .paymentMethod(method)
                        .feePercent(dbl(body.get("feePercent")))
                        .fee(dbl(body.get("fee")))
                        .totalAmountWithFee(total)
                        .build();
                if (seen.add(comboKey(combo))) {
                    combos.add(combo);
                }
            } catch (Exception e) {
                log.warn("[reviewContext] cannot parse orderRequestLog {} for {}: {}",
                        reqLog.getId(), orderNo, e.getMessage());
            }
        }
        return combos;
    }

    private List<ReceivedPayment> buildReceivedPayments(Orders order) {
        List<ReceivedPayment> out = new ArrayList<>();
        for (PaymentWebhookLog logEntry : webhookLogsFor(order)) {
            String provider = logEntry.getPaymentProvider();
            Map<String, Object> payload = parsePayload(logEntry.getPayloadJson());
            out.add(ReceivedPayment.builder()
                    .provider(provider)
                    .transactionId(logEntry.getTransactionId())
                    .resolvedMethod(paymentWebhookService.resolveMethodFromWebhookLog(provider, logEntry.getPayloadJson()))
                    .amount(logEntry.getAmount())
                    .payerName(str(payload.get("payerName")))
                    .reference(firstNonNull(str(payload.get("billPaymentRef1")), str(payload.get("invoiceNo")),
                            str(payload.get("referenceNo"))))
                    .receivedDateTime(logEntry.getReceivedDateTime())
                    .logType(logEntry.getLogType())
                    .reasonType(logEntry.getReasonType())
                    .successful(paymentWebhookService.isSuccessfulPaymentLog(provider, logEntry.getPayloadJson()))
                    .build());
        }
        return out;
    }

    private List<PaymentWebhookLog> webhookLogsFor(Orders order) {
        Map<Long, PaymentWebhookLog> unique = new LinkedHashMap<>();
        for (PaymentWebhookLog logEntry : webhookLogRepository.findByOrderNo(order.getOrderNo())) {
            unique.put(logEntry.getId(), logEntry);
        }
        if (order.getUuid() != null) {
            for (PaymentWebhookLog logEntry : webhookLogRepository.findByOrderNo(order.getUuid())) {
                unique.put(logEntry.getId(), logEntry);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private List<TimelineEvent> buildTimeline(Orders order) {
        List<TimelineEvent> events = new ArrayList<>();
        for (OrderRequestLog reqLog : orderRequestLogRepository.findByOrderNoOrderByIdAsc(order.getOrderNo())) {
            Map<String, Object> body = parsePayload(reqLog.getRequestBody());
            if ("UPDATE_PAYMENT".equalsIgnoreCase(reqLog.getRequestType())) {
                events.add(TimelineEvent.builder()
                        .time(reqLog.getCreatedTime())
                        .type("PAYMENT_METHOD")
                        .method(str(body.get("paymentMethod")))
                        .feePercent(dbl(body.get("feePercent")))
                        .amount(dbl(body.get("totalAmountWithFee")))
                        .build());
            } else {
                events.add(TimelineEvent.builder()
                        .time(reqLog.getCreatedTime())
                        .type("CREATE")
                        .amount(dbl(body.get("totalPrice")))
                        .build());
            }
        }
        for (PaymentWebhookLog logEntry : webhookLogsFor(order)) {
            events.add(TimelineEvent.builder()
                    .time(logEntry.getReceivedDateTime())
                    .type(WebhookLogType.ANOMALY.equals(logEntry.getLogType()) ? "ANOMALY" : "WEBHOOK")
                    .provider(logEntry.getPaymentProvider())
                    .method(paymentWebhookService.resolveMethodFromWebhookLog(
                            logEntry.getPaymentProvider(), logEntry.getPayloadJson()))
                    .amount(logEntry.getAmount())
                    .reasonType(logEntry.getReasonType())
                    .build());
        }
        events.sort(Comparator.comparing(TimelineEvent::getTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    private List<FieldChange> buildExpectedChanges(Orders order, OfferedCombo combo, String txnId,
            OffsetDateTime paidAt) {
        List<FieldChange> changes = new ArrayList<>();
        changes.add(change("paymentMethod", order.getPaymentMethod(), combo.getPaymentMethod()));
        changes.add(change("feePercent", order.getFeePercent(), combo.getFeePercent()));
        changes.add(change("fee", order.getFee(), combo.getFee()));
        changes.add(change("totalAmountWithFee", order.getTotalAmountWithFee(), combo.getTotalAmountWithFee()));
        changes.add(change("paymentStatus", order.getPaymentStatus(), PaymentStatus.SUCCESS.toString()));
        changes.add(change("reviewReason", order.getReviewReason(), null));
        changes.add(change("scbTransactionId", order.getScbTransactionId(),
                notBlank(txnId) ? txnId : order.getScbTransactionId()));
        changes.add(change("paymentDateTime", order.getPaymentDateTime(),
                paidAt != null ? paidAt : order.getPaymentDateTime()));
        return changes;
    }

    private FieldChange change(String field, Object current, Object after) {
        String currentText = current == null ? null : String.valueOf(current);
        String afterText = after == null ? null : String.valueOf(after);
        return FieldChange.builder()
                .field(field)
                .current(currentText)
                .after(afterText)
                .changed(!Objects.equals(currentText, afterText))
                .build();
    }

    private Map<String, Object> parsePayload(String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<ReceivedPayment> successfulMatchedPayments(List<ReceivedPayment> received,
            List<OfferedCombo> combos) {
        List<ReceivedPayment> out = new ArrayList<>();
        Set<String> seenTxn = new HashSet<>();
        for (ReceivedPayment rp : received) {
            if (!rp.isSuccessful() || rp.getAmount() == null) {
                continue;
            }
            if (rp.getTransactionId() != null && !seenTxn.add(rp.getTransactionId())) {
                continue;
            }
            if (matchCombo(rp.getAmount(), rp.getResolvedMethod(), combos) != null) {
                out.add(rp);
            }
        }
        return out;
    }

    private OfferedCombo matchCombo(Double amount, String method, List<OfferedCombo> combos) {
        if (amount == null) {
            return null;
        }
        List<OfferedCombo> within = new ArrayList<>();
        for (OfferedCombo combo : combos) {
            if (combo.getTotalAmountWithFee() == null) {
                continue;
            }
            if (Math.abs(combo.getTotalAmountWithFee() - amount) < AMOUNT_TOLERANCE) {
                if (method != null && method.equalsIgnoreCase(combo.getPaymentMethod())) {
                    return combo;
                }
                within.add(combo);
            }
        }
        return within.size() == 1 ? within.get(0) : null;
    }

    private OfferedCombo syntheticCombo(String method, Double amount, Double basePrice) {
        Double fee = null;
        Double feePercent = null;
        if (basePrice != null) {
            fee = round2(amount - basePrice);
            if (basePrice > 0) {
                feePercent = round2((amount - basePrice) / basePrice * 100.0);
            }
        }
        return OfferedCombo.builder()
                .paymentMethod(method)
                .feePercent(feePercent)
                .fee(fee)
                .totalAmountWithFee(amount)
                .build();
    }

    private PaymentWebhookLog findLogForOrderTxn(Orders order, String transactionId) {
        if (!notBlank(transactionId)) {
            return null;
        }
        for (PaymentWebhookLog logEntry : webhookLogRepository.findByTransactionId(transactionId)) {
            if (orderMatches(logEntry, order)) {
                return logEntry;
            }
        }
        return null;
    }

    private boolean orderMatches(PaymentWebhookLog logEntry, Orders order) {
        String logOrderNo = logEntry.getOrderNo();
        return logOrderNo != null
                && (logOrderNo.equals(order.getOrderNo()) || logOrderNo.equals(order.getUuid()));
    }

    private PaymentReviewAudit buildAudit(String orderNo, Integer adminId, String action, String outcome,
            String transactionId, String beforeJson, String afterJson, String note) {
        PaymentReviewAudit audit = new PaymentReviewAudit();
        audit.setOrderNo(orderNo);
        audit.setAdminUserId(adminId);
        audit.setAction(action);
        audit.setOutcome(outcome);
        audit.setTransactionId(transactionId);
        audit.setBeforeJson(beforeJson);
        audit.setAfterJson(afterJson);
        audit.setAdminNote(note);
        audit.setCreatedDateTime(OffsetDateTime.now());
        return audit;
    }

    private void writeAudit(String orderNo, Integer adminId, String action, String outcome,
            String transactionId, String beforeJson, String afterJson, String note) {
        try {
            auditRepository.save(
                    buildAudit(orderNo, adminId, action, outcome, transactionId, beforeJson, afterJson, note));
        } catch (Exception e) {
            log.error("[resolveReview] audit save failed for {}: {}", orderNo, e.getMessage());
        }
    }

    private ResolveReviewResponse response(boolean success, String outcome, String message,
            String orderNo, Orders order) {
        return ResolveReviewResponse.builder()
                .success(success)
                .outcome(outcome)
                .message(message)
                .orderNo(orderNo)
                .newState(toState(order))
                .build();
    }

    private OrderState toState(Orders order) {
        return OrderState.builder()
                .paymentStatus(order.getPaymentStatus())
                .reviewReason(order.getReviewReason())
                .paymentMethod(order.getPaymentMethod())
                .feePercent(order.getFeePercent())
                .fee(order.getFee())
                .totalAmountWithFee(order.getTotalAmountWithFee())
                .scbTransactionId(order.getScbTransactionId())
                .paymentDateTime(order.getPaymentDateTime())
                .build();
    }

    private Integer currentAdminUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return Integer.valueOf(auth.getName());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String comboKey(OfferedCombo combo) {
        return combo.getPaymentMethod() + "|" + combo.getFeePercent() + "|" + combo.getFee() + "|"
                + combo.getTotalAmountWithFee();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double dbl(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
