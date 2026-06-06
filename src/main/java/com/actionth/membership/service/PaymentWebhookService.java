package com.actionth.membership.service;

import com.actionth.membership.constant.PaymentProvider;
import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.constant.WebhookDescription;
import com.actionth.membership.constant.WebhookLogType;
import com.actionth.membership.constant.WebhookReasonType;
import com.actionth.membership.model.*;
import com.actionth.membership.model.request.TemplateEmailRequest;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.repository.PaymentWebhookLogRepository;
import com.actionth.membership.utils.AgeGroupUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import thaibaht.ThaiBaht;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final PaymentWebhookLogRepository webhookLogRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final EmailService emailService;
    private final AWSService awsService;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    private static final String WEBHOOK_DEDUP_PREFIX = "webhook:dedup:";
    private static final Duration WEBHOOK_DEDUP_TTL = Duration.ofHours(1);

    public void saveWebhookLog(String payload, String transactionId, String orderNo,
            Orders order, Double amount, String currency, String paymentProvider,
            String logType, String reasonType) {
        PaymentWebhookLog webhookLog = new PaymentWebhookLog();
        webhookLog.setLogType(logType);
        webhookLog.setPaymentProvider(paymentProvider);
        webhookLog.setPayloadJson(payload);
        webhookLog.setTransactionId(transactionId);
        webhookLog.setOrderNo(orderNo);
        webhookLog.setReceivedDateTime(OffsetDateTime.now());
        webhookLog.setReasonType(reasonType);

        if (order != null) {
            webhookLog.setOrderId(order.getId());
            webhookLog.setPaymentStatusAtWebhookTime(order.getPaymentStatus());
            webhookLog.setAmount(amount != null ? amount : order.getTotalAmountWithFee());
            webhookLog.setCurrency(currency);
        } else {
            webhookLog.setAmount(amount);
            webhookLog.setCurrency(currency);
        }

        if (WebhookLogType.ANOMALY.equals(logType) && reasonType != null) {
            webhookLog.setDescription(WebhookDescription.buildDescription(paymentProvider, reasonType, orderNo));
        } else {
            webhookLog.setDescription(WebhookDescription.formatWebhook(paymentProvider));
        }

        webhookLogRepository.save(webhookLog);
    }

    public List<String> getPayloadsByOrderNo(String orderNo) {
        return webhookLogRepository.findByOrderNo(orderNo)
                .stream()
                .map(PaymentWebhookLog::getPayloadJson)
                .toList();
    }

    /**
     * Get payloads by order key (supports both direct orderNo and qrId)
     */
    public List<String> getPayloadsByOrderKey(String key) {
        List<String> payloads = getPayloadsByOrderNo(key);

        if (payloads.isEmpty()) {
            String orderNoFromQrId = extractOrderNoFromQrId(key);
            if (orderNoFromQrId != null && !orderNoFromQrId.isBlank()) {
                payloads = getPayloadsByOrderNo(orderNoFromQrId.trim());
            }
        }

        return payloads;
    }

    /**
     * Process SCB webhook payload
     */
    public Map<String, Object> processSCBWebhook(Map<String, Object> payload) throws Exception {
        String jsonPayload = objectMapper.writeValueAsString(payload);
        String transactionId = safeString(payload.get("transactionId"));
        String ref1 = safeString(payload.get("billPaymentRef1")); // PromptPay (qrcode)
        String qrId = safeString(payload.get("qrId")); // Alipay/WeChatPay
        Double webhookAmount = parseDouble(payload.get("amount"));

        if (!isBlank(transactionId) && !tryClaimWebhook("SCB", transactionId)) {
            log.info("[SCB webhook] Duplicate transactionId={}, skipping", transactionId);
            return createAckResponse(transactionId);
        }

        String orderNoFromQr = extractOrderNoFromQrId(qrId);
        String orderKey = firstNonBlank(trimToNull(ref1), trimToNull(orderNoFromQr));

        Orders order = null;
        String logType = WebhookLogType.WEBHOOK;
        String reasonType = null;

        try {
            if (isBlank(orderKey)) {
                log.warn("[SCB webhook] Missing order key. txnId={}, ref1={}, qrId={}", transactionId, ref1, qrId);
                return createAckResponse(transactionId);
            }

            order = orderRepository.findByOrderNo(orderKey).orElse(null);

            if (order == null) {
                log.warn("[SCB webhook] Order not found. orderKey={}, txnId={}", orderKey, transactionId);
                return createAckResponse(transactionId);
            }

            String currentStatus = order.getPaymentStatus();

            if (!PaymentStatus.PENDING.toString().equalsIgnoreCase(currentStatus)) {
                logType = WebhookLogType.ANOMALY;
                reasonType = determineReasonType(currentStatus);
                return createAckResponse(transactionId);
            }

            OffsetDateTime due = order.getPaymentDueDatetime();
            if (due != null && due.isBefore(OffsetDateTime.now())) {
                log.warn("[SCB webhook] ignore overdue. orderNo={}, due={}, txnId={}",
                        order.getOrderNo(), due, transactionId);

                return createAckResponse(transactionId);
            }

            // Block if payment method or amount mismatch — flag for admin review
            String currentPaymentMethod = order.getPaymentMethod();
            String resolvedMethod = resolveScbPaymentMethod(ref1, qrId, payload);
            if (resolvedMethod == null) {
                log.warn("[SCB webhook] Unknown payment method: orderNo={}, currentMethod={}, txnId={}. Blocking for review.",
                        order.getOrderNo(), currentPaymentMethod, transactionId);
                order.setPaymentStatus(PaymentStatus.REVIEW.toString());
                order.setReviewReason(WebhookReasonType.UNKNOWN_PAYMENT_METHOD);
                order.setScbTransactionId(transactionId);
                orderRepository.save(order);
                logType = WebhookLogType.ANOMALY;
                reasonType = WebhookReasonType.UNKNOWN_PAYMENT_METHOD;
                return createAckResponse(transactionId);
            }
            if (!resolvedMethod.equalsIgnoreCase(currentPaymentMethod)) {
                log.warn("[SCB webhook] Payment method mismatch: orderNo={}, currentMethod={}, resolvedMethod={}, txnId={}. " +
                        "Blocking order for admin review.",
                        order.getOrderNo(), currentPaymentMethod, resolvedMethod, transactionId);
                order.setPaymentStatus(PaymentStatus.REVIEW.toString());
                order.setReviewReason(WebhookReasonType.PAYMENT_METHOD_MISMATCH);
                order.setScbTransactionId(transactionId);
                orderRepository.save(order);
                logType = WebhookLogType.ANOMALY;
                reasonType = WebhookReasonType.PAYMENT_METHOD_MISMATCH;
                return createAckResponse(transactionId);
            }

            if (webhookAmount != null && order.getTotalAmountWithFee() != null
                    && Math.abs(webhookAmount - order.getTotalAmountWithFee()) >= 1.0) {
                log.warn("[SCB webhook] Amount mismatch: orderNo={}, expected={}, received={}, txnId={}. " +
                        "Blocking order for admin review.",
                        order.getOrderNo(), order.getTotalAmountWithFee(), webhookAmount, transactionId);
                order.setPaymentStatus(PaymentStatus.REVIEW.toString());
                order.setReviewReason(WebhookReasonType.AMOUNT_MISMATCH);
                order.setScbTransactionId(transactionId);
                orderRepository.save(order);
                logType = WebhookLogType.ANOMALY;
                reasonType = WebhookReasonType.AMOUNT_MISMATCH;
                return createAckResponse(transactionId);
            }

            // Normal flow — no mismatch
            order.setPaymentStatus(PaymentStatus.SUCCESS.toString());
            order.setScbTransactionId(transactionId);
            order.setPaymentDateTime(OffsetDateTime.now());
            orderRepository.save(order);

            sendSuccessEmail(order, transactionId);

            return createAckResponse(transactionId);
        } finally {
            safeSaveWebhookLog(jsonPayload, transactionId, orderKey, order, webhookAmount, "THB",
                    PaymentProvider.SCB, logType, reasonType);
        }
    }

    /**
     * Process 2C2P webhook payload
     */
    public Map<String, Object> process2C2PWebhook(Map<String, Object> body) throws Exception {
        String jwt = safeString(body.get("payload"));
        Map<String, Object> normalized = decodeJwtNoVerify(jwt);

        if (normalized == null) {
            throw new IllegalArgumentException("invalid JWT payload");
        }

        String earlyTxnId = safeString(normalized.get("tranRef"));

        if (!isBlank(earlyTxnId) && !tryClaimWebhook("2C2P", earlyTxnId)) {
            log.info("[2C2P] Duplicate tranRef={}, skipping", earlyTxnId);
            return Map.of("ok", true, "message", "Duplicate webhook, skipped");
        }

        String orderRef = firstNonBlank(
                safeString(normalized.get("orderNo")),
                safeString(normalized.get("invoiceNo")),
                safeString(normalized.get("referenceNo")),
                safeString(normalized.get("tranRef")));

        String txnId = firstNonBlank(safeString(normalized.get("tranRef")), orderRef);

        String respCode = safeString(normalized.get("respCode"));
        String respDesc = safeString(normalized.get("respDesc"));
        Double amount = parseDouble(normalized.get("amount"));
        String currency = safeString(normalized.get("currencyCode"));

        if (isBlank(orderRef)) {
            throw new IllegalArgumentException("missing required fields (orderRef)");
        }

        String jsonPayload = objectMapper.writeValueAsString(normalized);
        Orders order = null;
        String logType = WebhookLogType.WEBHOOK;
        String reasonType = null;

        try {
            order = orderRepository.findByUuidOrOrderNo(orderRef).orElse(null);
            if (order == null) {
                log.warn("[2C2P] Order not found for orderRef={}", orderRef);
                return Map.of(
                        "ok", true,
                        "message", "Order not found, webhook logged");
            }

            String currentStatus = safeString(order.getPaymentStatus());

            if (!PaymentStatus.PENDING.toString().equalsIgnoreCase(currentStatus)) {
                logType = WebhookLogType.ANOMALY;
                reasonType = determineReasonType(currentStatus);

                return Map.of(
                        "ok", true,
                        "parsed", Map.of(
                                "orderRef", orderRef,
                                "respCode", respCode,
                                "respDesc", respDesc,
                                "anomalyLogged", true,
                                "anomalyReasonType", reasonType,
                                "currentStatus", currentStatus));
            }

            String code = (respCode == null) ? "" : respCode.trim();

            boolean isSuccess = "0000".equals(code);
            boolean isPending = "0001".equals(code) || "2001".equals(code);

            if (isSuccess) {
                // Block if payment method or amount mismatch — flag for admin review
                String currentPaymentMethod = order.getPaymentMethod();
                String resolvedMethod = resolve2c2pPaymentMethod(normalized);
                if (resolvedMethod == null) {
                    log.warn("[2C2P] Unknown payment method: orderRef={}, currentMethod={}, txnId={}. Blocking for review.",
                            orderRef, currentPaymentMethod, txnId);
                    order.setPaymentStatus(PaymentStatus.REVIEW.toString());
                    order.setReviewReason(WebhookReasonType.UNKNOWN_PAYMENT_METHOD);
                    orderRepository.save(order);
                    logType = WebhookLogType.ANOMALY;
                    reasonType = WebhookReasonType.UNKNOWN_PAYMENT_METHOD;
                    return Map.of(
                            "ok", true,
                            "parsed", Map.of(
                                    "orderRef", orderRef,
                                    "respCode", respCode,
                                    "respDesc", respDesc,
                                    "status", "REVIEW",
                                    "reason", "UNKNOWN_PAYMENT_METHOD",
                                    "currentMethod", currentPaymentMethod != null ? currentPaymentMethod : ""));
                }
                if (!resolvedMethod.equalsIgnoreCase(currentPaymentMethod)) {
                    log.warn("[2C2P] Payment method mismatch: orderRef={}, currentMethod={}, resolvedMethod={}, txnId={}. " +
                            "Blocking order for admin review.",
                            orderRef, currentPaymentMethod, resolvedMethod, txnId);
                    order.setPaymentStatus(PaymentStatus.REVIEW.toString());
                    order.setReviewReason(WebhookReasonType.PAYMENT_METHOD_MISMATCH);
                    orderRepository.save(order);
                    logType = WebhookLogType.ANOMALY;
                    reasonType = WebhookReasonType.PAYMENT_METHOD_MISMATCH;
                    return Map.of(
                            "ok", true,
                            "parsed", Map.of(
                                    "orderRef", orderRef,
                                    "respCode", respCode,
                                    "respDesc", respDesc,
                                    "status", "REVIEW",
                                    "reason", "PAYMENT_METHOD_MISMATCH",
                                    "currentMethod", currentPaymentMethod != null ? currentPaymentMethod : "",
                                    "resolvedMethod", resolvedMethod));
                }

                if (amount != null && order.getTotalAmountWithFee() != null
                        && Math.abs(amount - order.getTotalAmountWithFee()) >= 1.0) {
                    log.warn("[2C2P] Amount mismatch: orderRef={}, expected={}, received={}, txnId={}. " +
                            "Blocking order for admin review.",
                            orderRef, order.getTotalAmountWithFee(), amount, txnId);
                    order.setPaymentStatus(PaymentStatus.REVIEW.toString());
                    order.setReviewReason(WebhookReasonType.AMOUNT_MISMATCH);
                    orderRepository.save(order);
                    logType = WebhookLogType.ANOMALY;
                    reasonType = WebhookReasonType.AMOUNT_MISMATCH;
                    return Map.of(
                            "ok", true,
                            "parsed", Map.of(
                                    "orderRef", orderRef,
                                    "respCode", respCode,
                                    "respDesc", respDesc,
                                    "status", "REVIEW",
                                    "reason", "AMOUNT_MISMATCH",
                                    "expected", order.getTotalAmountWithFee(),
                                    "received", amount));
                }

                try {
                    orderService.updatePaymentStatus(orderRef, txnId, PaymentStatus.SUCCESS);
                } catch (Exception ex) {
                    log.error("[2C2P] updatePaymentStatus(SUCCESS) error: {}", ex.getMessage());
                }

                try {
                    Orders refreshed = orderRepository.findByUuidOrOrderNo(orderRef).orElse(order);
                    sendSuccessEmail(refreshed, txnId);
                } catch (Exception ex) {
                    log.error("[2C2P] sendSuccessEmail error: {}", ex.getMessage());
                }

                return Map.of(
                        "ok", true,
                        "parsed", Map.of(
                                "orderRef", orderRef,
                                "respCode", respCode,
                                "respDesc", respDesc,
                                "status", "SUCCESS",
                                "amount", amount,
                                "currency", currency));
            }

            if (isPending) {
                log.info("[2C2P] Pending; orderRef={}, respCode={}", orderRef, respCode);

                return Map.of(
                        "ok", true,
                        "parsed", Map.of(
                                "orderRef", orderRef,
                                "respCode", respCode,
                                "respDesc", respDesc,
                                "status", "PENDING",
                                "amount", amount,
                                "currency", currency));
            }

            // Failed payment (e.g. 4081 auth failed, 0003 cancelled, etc.)
            if (shouldLog2c2pAnomaly(respCode)) {
                logType = WebhookLogType.ANOMALY;
                reasonType = build2c2pReasonType(respCode);
            }

            log.info("[2C2P] Payment not successful; orderRef={}, respCode={}, respDesc={}", orderRef, respCode, respDesc);

            return Map.of(
                    "ok", true,
                    "parsed", Map.of(
                            "orderRef", orderRef,
                            "respCode", respCode,
                            "respDesc", respDesc,
                            "status", "FAILED",
                            "amount", amount,
                            "currency", currency,
                            "anomalyLogged", true));
        } finally {
            safeSaveWebhookLog(jsonPayload, txnId, orderRef, order, amount, currency,
                    PaymentProvider.TWO_C2P, logType, reasonType);
        }
    }

    /**
     * Check if there are any valid webhook logs for the order and update status if
     * found.
     * This replaces the frontend polling logic.
     */
    public boolean verifyAndSettleFromLogs(String orderNo) {
        Orders order = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (order == null)
            return false;

        if (PaymentStatus.SUCCESS.toString().equalsIgnoreCase(order.getPaymentStatus())) {
            return true;
        }

        List<String> payloads = getWebhookPayloadsByOrderKey(orderNo);
        for (String json : payloads) {
            try {
                Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
                });
                String amountStr = safeString(map.get("amount"));

                if (amountStr == null && map.containsKey("payload")) {
                    amountStr = extractAmountFromJwtPayload(map);
                }

                if (amountStr != null) {
                    double logAmount = Double.parseDouble(amountStr);
                    double orderAmount = order.getTotalAmountWithFee();

                    if (Math.abs(logAmount - orderAmount) < 1.0) {
                        String txnId = safeString(map.get("transactionId"));
                        if (txnId == null)
                            txnId = safeString(map.get("tranRef"));

                        orderService.updatePaymentStatus(orderNo, txnId, PaymentStatus.SUCCESS);

                        order.setPaymentStatus(PaymentStatus.SUCCESS.toString());
                        if (order.getPaymentDateTime() == null) {
                            order.setPaymentDateTime(OffsetDateTime.now());
                        }
                        sendSuccessEmail(order, txnId);
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing log for settle: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Send success email for an order
     */
    public void sendSuccessEmail(Orders order, String transactionId) {
        log.info("[Email] Starting sendSuccessEmail - orderNo={}, transactionId={}",
                order.getOrderNo(), transactionId);

        String eventName = (order.getEvent() != null && order.getEvent().getName() != null)
                ? order.getEvent().getName().trim()
                : "";
        log.info("[Email] Event name: {}", eventName);

        String subject = "ยืนยันการสมัครเข้าร่วมกิจกรรม " + eventName;

        Map<String, Object> variables = buildPaymentSuccessVariables(order, transactionId, false);
        log.info("[Email] Built variables - keys: {}", variables.keySet());
        log.debug("[Email] Full variables content: {}", variables);

        TemplateEmailRequest request = new TemplateEmailRequest();
        request.setTo(order.getCreatedBy().getEmail());
        request.setSubject(subject);
        request.setOrderId(order.getUuid());
        request.setTemplateName("payment-success");
        request.setVariables(variables);

        log.info("[Email] Sending email to: {}, subject: {}",
                order.getCreatedBy().getEmail(), subject);

        emailService.sendGeneralTemplateEmail(request);
        log.info("[Email] Email sent successfully for orderNo={}, uuid={}", order.getOrderNo(), order.getUuid());
    }

    /**
     * Resend payment success email with timezone clarification correction notice
     * Used when customer needs clarification about timezone information
     */
    public void resendSuccessEmailWithCorrection(Orders order) {
        log.info("[Email Resend] Starting resendSuccessEmailWithCorrection - orderNo={}, orderUuid={}",
                order.getOrderNo(), order.getUuid());

        // Validate order has been paid
        if (!"SUCCESS".equals(order.getPaymentStatus())) {
            log.warn("[Email Resend] Cannot resend - order payment status is not SUCCESS: {}", 
                    order.getPaymentStatus());
            throw new IllegalStateException("Cannot resend email for unpaid order");
        }

        String eventName = (order.getEvent() != null && order.getEvent().getName() != null)
                ? order.getEvent().getName().trim()
                : "";
        log.info("[Email Resend] Event name: {}", eventName);

        String subject = "[ประกาศสำคัญ] ยืนยันการสมัครเข้าร่วมกิจกรรม " + eventName;

        // Use last transaction ID or generate a resend marker
        String transactionId = (order.getScbTransactionId() != null && !order.getScbTransactionId().isEmpty())
                ? order.getScbTransactionId()
                : "RESEND-" + order.getOrderNo();

        Map<String, Object> variables = buildPaymentSuccessVariables(order, transactionId, true);
        log.info("[Email Resend] Built variables with showCorrection=true - keys: {}", variables.keySet());

        TemplateEmailRequest request = new TemplateEmailRequest();
        request.setTo(order.getCreatedBy().getEmail());
        request.setSubject(subject);
        request.setOrderId(order.getUuid());
        request.setTemplateName("payment-success");
        request.setVariables(variables);

        log.info("[Email Resend] Sending clarification email to: {}, subject: {}",
                order.getCreatedBy().getEmail(), subject);

        emailService.sendGeneralTemplateEmail(request);
        log.info("[Email Resend] Clarification email sent successfully for orderNo={}, uuid={}", 
                order.getOrderNo(), order.getUuid());
    }

    /**
     * Same as resendSuccessEmailWithCorrection but sends directly to RabbitMQ,
     * bypassing sendOrQueue. Used when called from email queue processing
     * to avoid creating duplicate OUTBOUND queue records.
     */
    public void resendSuccessEmailWithCorrectionDirect(Orders order) {
        resendSuccessEmailWithCorrectionDirect(order, null);
    }

    /**
     * Sends correction email directly to RabbitMQ, reusing an existing EmailLog if provided.
     * @return the EmailLog ID used (new or existing)
     */
    public Long resendSuccessEmailWithCorrectionDirect(Orders order, Long existingEmailLogId) {
        log.info("[Email Resend Direct] Starting for orderNo={}, orderUuid={}",
                order.getOrderNo(), order.getUuid());

        if (!"SUCCESS".equals(order.getPaymentStatus())) {
            log.warn("[Email Resend Direct] Cannot resend - payment status is not SUCCESS: {}",
                    order.getPaymentStatus());
            throw new IllegalStateException("Cannot resend email for unpaid order");
        }

        String eventName = (order.getEvent() != null && order.getEvent().getName() != null)
                ? order.getEvent().getName().trim()
                : "";

        String subject = "[ประกาศสำคัญ] ยืนยันการสมัครเข้าร่วมกิจกรรม " + eventName;

        String transactionId = (order.getScbTransactionId() != null && !order.getScbTransactionId().isEmpty())
                ? order.getScbTransactionId()
                : "RESEND-" + order.getOrderNo();

        Map<String, Object> variables = buildPaymentSuccessVariables(order, transactionId, true);

        TemplateEmailRequest request = new TemplateEmailRequest();
        request.setTo(order.getCreatedBy().getEmail());
        request.setSubject(subject);
        request.setOrderId(order.getUuid());
        request.setTemplateName("payment-success");
        request.setVariables(variables);

        Long logId = emailService.sendGeneralTemplateEmailDirect(request, existingEmailLogId);
        log.info("[Email Resend Direct] Clarification email dispatched for orderNo={}, uuid={}, logId={}",
                order.getOrderNo(), order.getUuid(), logId);
        return logId;
    }

    /**
     * Build email variables for payment success
     * @param order The order object
     * @param transactionId Payment transaction ID
     * @param showCorrection Whether to show correction notice about timezone confusion
     * @return Map of template variables
     */
    public Map<String, Object> buildPaymentSuccessVariables(Orders order, String transactionId, boolean showCorrection) {
        log.info("[Email Variables] Building variables for orderNo={}, uuid={}, showCorrection={}", 
                order.getOrderNo(), order.getUuid(), showCorrection);

        Map<String, Object> variables = new HashMap<>();
        initializeDefaultVariables(variables);
        log.debug("[Email Variables] After initializeDefaultVariables: {}", variables.keySet());

        populateEventInfo(variables, order);
        log.debug("[Email Variables] After populateEventInfo - eventName={}, coverImg={}",
                variables.get("eventName"), variables.get("coverImg"));

        variables.put("refNo", order.getOrderNo());
        variables.put("transactionId", transactionId);
        variables.put("showCorrection", showCorrection);
        log.debug("[Email Variables] refNo={}, transactionId={}, showCorrection={}",
                order.getOrderNo(), transactionId, showCorrection);

        populateOrderDate(variables, order);
        log.debug("[Email Variables] After populateOrderDate - dateRegister={}",
                variables.get("dateRegister"));

        populateOrderFinancials(variables, order);
        log.debug(
                "[Email Variables] After populateOrderFinancials - totalPrice={}, totalAmountWithFee={}, feeAmount={}",
                variables.get("totalPrice"), variables.get("totalAmountWithFee"), variables.get("feeAmount"));

        processApplicantsAndTotals(variables, order);
        log.debug("[Email Variables] After processApplicantsAndTotals - applicant count={}",
                variables.get("applicants") != null ? ((List<?>) variables.get("applicants")).size() : 0);

        generateThaiBahtText(variables, order);
        log.debug("[Email Variables] After generateThaiBahtText - thaiBahtText={}",
                variables.get("thaiBahtText"));

        log.info("[Email Variables] Completed building variables with {} keys", variables.size());

        return variables;
    }

    private void initializeDefaultVariables(Map<String, Object> variables) {
        variables.put("eventName", "-");
        variables.put("coverImg", "");
        variables.put("eventDetailsHtml", "");
        variables.put("eventDate", "");
        variables.put("thaiBahtText", "");
    }

    private void populateEventInfo(Map<String, Object> variables, Orders order) {
        log.debug("[Email EventInfo] Starting populateEventInfo");

        if (order.getEvent() != null) {
            log.debug("[Email EventInfo] Event found - name={}", order.getEvent().getName());

            variables.put("eventName", order.getEvent().getName());
            variables.put("coverImg", resolveCoverImg(order));
            variables.put("eventDetailsHtml", resolveEventDetails(order));

            if (order.getEvent().getEventDate() != null) {
                String formattedDate = order.getEvent().getEventDate()
                        .atZoneSameInstant(ZoneId.of("Asia/Bangkok"))
                        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("th", "TH")));
                variables.put("eventDate", formattedDate);
                log.debug("[Email EventInfo] Event date formatted: {}", formattedDate);
            } else {
                log.warn("[Email EventInfo] Event date is NULL");
            }
        } else {
            log.warn("[Email EventInfo] Order event is NULL for orderNo={}", order.getOrderNo());
        }
    }

    private String resolveCoverImg(Orders order) {
        String coverImg = order.getEvent().getPictureUrl();
        log.debug("[Email CoverImg] Original pictureUrl: {}", coverImg);

        if (coverImg != null && !coverImg.startsWith("http")) {
            try {
                String prefix = order.getEvent().getPrefixPath() != null ? order.getEvent().getPrefixPath() : "";
                log.debug("[Email CoverImg] Generating permanent public URL with prefix: {}", prefix);
                String publicUrl = awsService.makePublicAndGetUrl(prefix, coverImg);
                log.debug("[Email CoverImg] Generated permanent public URL: {}", publicUrl);
                return publicUrl;
            } catch (Exception e) {
                log.warn("[Email CoverImg] Failed to generate public URL for event image", e);
            }
        }
        return coverImg;
    }

    private String resolveEventDetails(Orders order) {
        String finalEventDetailsHtml = "";
        String prefix = order.getEvent().getPrefixPath() != null ? order.getEvent().getPrefixPath() : "event";

        finalEventDetailsHtml = order.getEvent().getDescription();
        finalEventDetailsHtml = awsService.resolveImagePlaceholders(finalEventDetailsHtml, prefix);

        return finalEventDetailsHtml != null ? finalEventDetailsHtml : "";
    }

    private void populateOrderDate(Map<String, Object> variables, Orders order) {
        if (order.getPaymentDateTime() != null) {
            String formattedDate = order.getPaymentDateTime()
                    .atZoneSameInstant(ZoneId.of("Asia/Bangkok"))
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm น.", new Locale("th", "TH")));
            variables.put("dateRegister", formattedDate);
            log.debug("[Email OrderDate] Payment date formatted: {}", formattedDate);
        } else {
            variables.put("dateRegister", "-");
            log.warn("[Email OrderDate] Payment date is NULL for orderNo={}", order.getOrderNo());
        }
    }

    private void populateOrderFinancials(Map<String, Object> variables, Orders order) {
        log.debug("[Email Financials] totalPrice={}, totalAmountWithFee={}, fee={}, feePercent={}",
                order.getTotalPrice(), order.getTotalAmountWithFee(), order.getFee(), order.getFeePercent());

        variables.put("totalPrice", order.getTotalPrice());
        variables.put("totalAmountWithFee", order.getTotalAmountWithFee());
        variables.put("feeAmount", order.getFee());
        variables.put("feePercent", order.getFeePercent());
    }

    private void processApplicantsAndTotals(Map<String, Object> variables, Orders order) {
        log.debug("[Email Applicants] Processing applicants for orderNo={}", order.getOrderNo());

        double totalRegFee = 0.0;
        double totalShipping = 0.0;
        double totalDiscountNoShirt = 0.0;
        double totalCoupon = 0.0;

        List<Map<String, Object>> applicants = new ArrayList<>();
        if (order.getOrderDetails() != null) {
            log.debug("[Email Applicants] Found {} order details", order.getOrderDetails().size());

            for (OrderDetail d : order.getOrderDetails()) {
                Map<String, Object> applicantMap = createApplicantMap(d);
                applicants.add(applicantMap);
                log.debug("[Email Applicants] Added applicant: {} {}, eventType={}, price={}",
                        d.getFirstName(), d.getLastName(),
                        d.getEventType() != null ? d.getEventType().getName() : "null",
                        d.getPrice());

                totalRegFee += d.getPrice() != null ? d.getPrice() : 0.0;
                totalShipping += d.getShippingFee() != null ? d.getShippingFee() : 0.0;
                totalDiscountNoShirt += d.getDiscountShirt() != null ? d.getDiscountShirt() : 0.0;
                totalCoupon += d.getCouponDiscount() != null ? d.getCouponDiscount() : 0.0;
            }
        } else {
            log.warn("[Email Applicants] Order details is NULL for orderNo={}", order.getOrderNo());
        }

        variables.put("applicants", applicants);
        variables.put("totalRegFee", totalRegFee);
        variables.put("totalShipping", totalShipping);
        variables.put("totalDiscountNoShirt", totalDiscountNoShirt);
        double finalTotalCoupon = (order.getCouponDiscount() != null && order.getCouponDiscount() > 0)
                ? order.getCouponDiscount()
                : totalCoupon;
        variables.put("totalCoupon", finalTotalCoupon);

        log.debug("[Email Applicants] Totals - regFee={}, shipping={}, discountNoShirt={}, coupon={}",
                totalRegFee, totalShipping, totalDiscountNoShirt, finalTotalCoupon);
    }

    private Map<String, Object> createApplicantMap(OrderDetail d) {
        log.debug("[Email Applicant] Creating map for: {} {}", d.getFirstName(), d.getLastName());

        Map<String, Object> app = new HashMap<>();
        app.put("firstName", d.getFirstName());
        app.put("lastName", d.getLastName());
        app.put("eventTypeName", d.getEventType() != null ? d.getEventType().getName() : "-");
        app.put("ageGroupName", AgeGroupUtils.resolveAgeGroup(d));
        app.put("deliveryMethod", d.getDeliveryMethod());
        app.put("teamClub", d.getTeamClub());

        log.debug("[Email Applicant] EventType={}, AgeGroup={}, DeliveryMethod={}",
                d.getEventType() != null ? d.getEventType().getName() : "null",
                app.get("ageGroupName"), d.getDeliveryMethod());

        populateShirtInfo(app, d);
        populateApplicantFinancials(app, d);

        return app;
    }

    private void populateShirtInfo(Map<String, Object> app, OrderDetail d) {
        app.put("shirtSizeName", null);
        app.put("shirtTypeName", null);
        boolean noShirt = true;

        if (d.getShirtSize() != null) {
            String shirtSizeName = d.getShirtSize().getName();
            app.put("shirtSizeName", shirtSizeName);
            log.debug("[Email ShirtInfo] ShirtSize={}", shirtSizeName);

            if (d.getShirtType() != null) {
                String shirtTypeName = d.getShirtType().getName();
                app.put("shirtTypeName", shirtTypeName);
                log.debug("[Email ShirtInfo] ShirtType={}", shirtTypeName);
            }
            if (!"No Shirt".equalsIgnoreCase(shirtSizeName)) {
                noShirt = false;
            }
        } else {
            log.debug("[Email ShirtInfo] No shirt size for applicant");
        }
        app.put("noShirt", noShirt);
        log.debug("[Email ShirtInfo] noShirt={}", noShirt);
    }

    private void populateApplicantFinancials(Map<String, Object> app, OrderDetail d) {
        Double price = d.getPrice() != null ? d.getPrice() : 0.0;
        Double shippingFee = d.getShippingFee() != null ? d.getShippingFee() : 0.0;
        Double discountNoShirt = d.getDiscountShirt() != null ? d.getDiscountShirt() : 0.0;
        Double personalCoupon = d.getCouponDiscount() != null ? d.getCouponDiscount() : 0.0;

        log.debug("[Email ApplicantFinancials] price={}, shipping={}, discountNoShirt={}, coupon={}",
                price, shippingFee, discountNoShirt, personalCoupon);

        app.put("price", price);
        app.put("shippingFee", shippingFee);
        app.put("discountNoShirt", discountNoShirt);
        app.put("personalCouponDiscount", personalCoupon);
    }

    private void generateThaiBahtText(Map<String, Object> variables, Orders order) {
        try {
            Double amount = order.getTotalAmountWithFee();
            log.debug("[Email ThaiBaht] Generating text for amount: {}", amount);

            String bahtText = new ThaiBaht().getText(amount);
            variables.put("thaiBahtText", bahtText);
            log.debug("[Email ThaiBaht] Generated text: {}", bahtText);
        } catch (Exception e) {
            log.warn("[Email ThaiBaht] Failed to generate Thai Baht Text", e);
        }
    }

    // Helper methods

    private boolean tryClaimWebhook(String provider, String transactionId) {
        try {
            String key = WEBHOOK_DEDUP_PREFIX + provider + ":" + transactionId;
            RBucket<String> bucket = redissonClient.getBucket(key);
            boolean claimed = bucket.setIfAbsent("1");
            if (claimed) {
                bucket.expire(WEBHOOK_DEDUP_TTL);
            }
            return claimed;
        } catch (Exception e) {
            log.warn("[webhook dedup] Redis dedup check failed for {}:{}, allowing through",
                    provider, transactionId, e);
            return true;
        }
    }

    private Map<String, Object> createAckResponse(String transactionId) {
        Map<String, Object> ack = new HashMap<>();
        ack.put("resCode", "00");
        ack.put("resDesc", "success");
        if (transactionId != null)
            ack.put("transactionId", transactionId);
        return ack;
    }

    public String determineReasonType(String currentStatus) {
        if (currentStatus == null) {
            return WebhookReasonType.UNKNOWN_STATUS;
        }

        return switch (currentStatus.toUpperCase()) {
            case "SUCCESS" -> WebhookReasonType.DUPLICATE_AFTER_SUCCESS;
            case "FAILED" -> WebhookReasonType.PAYMENT_AFTER_FAILED;
            case "CANCELLED", "CANCELED", "CANCEL" -> WebhookReasonType.PAYMENT_AFTER_CANCELLED;
            default -> WebhookReasonType.UNKNOWN_STATUS;
        };
    }

    private boolean shouldLog2c2pAnomaly(String respCode) {
        return WebhookReasonType.is2c2pAnomaly(respCode);
    }

    private String build2c2pReasonType(String respCode) {
        return WebhookReasonType.build2c2pReasonType(respCode);
    }

    /**
     * Get only WEBHOOK-type payloads by order key (for settlement verification).
     * Excludes ANOMALY entries to prevent settling based on failed payment logs.
     */
    private List<String> getWebhookPayloadsByOrderKey(String key) {
        List<String> payloads = webhookLogRepository.findByOrderNoAndLogType(key, WebhookLogType.WEBHOOK)
                .stream()
                .map(PaymentWebhookLog::getPayloadJson)
                .toList();

        if (payloads.isEmpty()) {
            String orderNoFromQrId = extractOrderNoFromQrId(key);
            if (orderNoFromQrId != null && !orderNoFromQrId.isBlank()) {
                payloads = webhookLogRepository.findByOrderNoAndLogType(
                                orderNoFromQrId.trim(), WebhookLogType.WEBHOOK)
                        .stream()
                        .map(PaymentWebhookLog::getPayloadJson)
                        .toList();
            }
        }

        return payloads;
    }

    private void safeSaveWebhookLog(String jsonPayload, String transactionId, String orderNo,
            Orders order, Double amount, String currency, String paymentProvider,
            String logType, String reasonType) {
        try {
            saveWebhookLog(jsonPayload, transactionId, orderNo, order, amount, currency,
                    paymentProvider, logType, reasonType);
        } catch (Exception ex) {
            log.error("[webhook] saveWebhookLog failed. provider={}, txnId={}, orderNo={}",
                    paymentProvider, transactionId, orderNo, ex);
        }
    }

    private String trimToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private String extractOrderNoFromQrId(String qrId) {
        if (qrId == null)
            return null;
        String s = qrId.trim();

        if (s.length() == 20 && s.startsWith("SCB")) {
            String orderNo = s.substring(6);
            if (orderNo.length() == 14 && orderNo.startsWith("OR")) {
                return orderNo;
            }
            return null;
        }
        return null;
    }

    private String extractAmountFromJwtPayload(Map<String, Object> map) {
        try {
            Map<String, Object> decoded = decodeJwtNoVerify(safeString(map.get("payload")));
            if (decoded != null && "0000".equals(decoded.get("respCode"))) {
                return safeString(decoded.get("amount"));
            }
        } catch (Exception e) {
            log.error("extractAmountFromJwtPayload failed", e);
        }
        return null;
    }

    public static Map<String, Object> decodeJwtNoVerify(String jwt) {
        try {
            if (jwt == null || jwt.isBlank()) {
                throw new IllegalArgumentException("JWT string is null or empty");
            }
            String[] parts = jwt.split("\\.");
            if (parts.length < 2)
                throw new IllegalArgumentException("Bad JWT");
            String payloadPart = pad(parts[1]);
            String json = new String(
                    Base64.getUrlDecoder().decode(payloadPart),
                    StandardCharsets.UTF_8);
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot decode JWT: " + e.getMessage(), e);
        }
    }

    private static String pad(String s) {
        int m = s.length() % 4;
        return (m == 0) ? s : s + "====".substring(m);
    }

    private String safeString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Double parseDouble(Object o) {
        if (o == null)
            return null;
        try {
            return Double.valueOf(String.valueOf(o));
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals)
            if (!isBlank(v))
                return v;
        return null;
    }

    private String resolveScbPaymentMethod(String ref1, String qrId, Map<String, Object> payload) {
        if (!isBlank(ref1)) {
            return "qrcode";
        }
        if (!isBlank(qrId)) {
            String pm = safeString(payload.get("paymentMethod"));
            if (pm != null && pm.toUpperCase().contains("ALIPAY")) {
                return "alipay";
            }
            if (pm != null && pm.toUpperCase().contains("WECHAT")) {
                return "wechatpay";
            }
            String channelCode = safeString(payload.get("channelCode"));
            if ("ALIPY".equalsIgnoreCase(channelCode)) {
                return "alipay";
            }
        }
        return null;
    }

    private String resolve2c2pPaymentMethod(Map<String, Object> payload) {
        String cardType = safeString(payload.get("cardType"));
        if (cardType != null) {
            return "creditcard";
        }

        String channelCode = safeString(payload.get("channelCode"));
        if (channelCode != null) {
            String code = channelCode.trim().toUpperCase();
            if ("TM".equals(code) || "LP".equals(code)) {
                return "ewallet";
            }
        }

        return null;
    }


}
