package com.actionth.membership.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.ConfirmationPreviewDto;
import com.actionth.membership.model.dto.PendingConfirmationDto;
import com.actionth.membership.model.dto.ResendResultDto;
import com.actionth.membership.model.request.TemplateEmailRequest;
import com.actionth.membership.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderConfirmationService {

    private final OrderRepository orderRepository;
    private final PaymentWebhookService paymentWebhookService;
    private final EmailSenderService emailSenderService;
    private final EmailService emailService;

    public List<PendingConfirmationDto> listMissingConfirmations() {
        List<PendingConfirmationDto> out = new ArrayList<>();
        for (Map<String, Object> row : orderRepository.findSuccessOrdersMissingConfirmation()) {
            int failed = toInt(row.get("failedCount"));
            int pending = toInt(row.get("pendingCount"));
            String status = failed > 0 ? "FAILED" : (pending > 0 ? "PENDING" : "NEVER_SENT");
            out.add(PendingConfirmationDto.builder()
                    .orderNo(str(row.get("orderNo")))
                    .eventName(str(row.get("eventName")))
                    .customerEmail(str(row.get("customerEmail")))
                    .paidAt(str(row.get("paidAt")))
                    .emailStatus(status)
                    .failedCount(failed)
                    .build());
        }
        return out;
    }

    public ConfirmationPreviewDto previewConfirmation(String orderNo) {
        Orders order = loadSuccessOrder(orderNo);
        Map<String, Object> variables = paymentWebhookService.buildPaymentSuccessVariables(order, txnIdOf(order), false);
        String html = emailSenderService.processTemplate("payment-success", variables);
        return ConfirmationPreviewDto.builder()
                .orderNo(orderNo)
                .recipientTo(order.getCreatedBy() != null ? order.getCreatedBy().getEmail() : null)
                .subject("ยืนยันการสมัครเข้าร่วมกิจกรรม " + eventName(order))
                .html(html)
                .build();
    }

    public ResendResultDto resendConfirmation(String orderNo, String testRecipient) {
        try {
            Orders order = loadSuccessOrder(orderNo);
            String txnId = txnIdOf(order);

            if (notBlank(testRecipient)) {
                Map<String, Object> variables = paymentWebhookService.buildPaymentSuccessVariables(order, txnId, false);
                TemplateEmailRequest request = new TemplateEmailRequest();
                request.setTo(testRecipient.trim());
                request.setSubject("[ทดสอบ] ยืนยันการสมัครเข้าร่วมกิจกรรม " + eventName(order));
                request.setTemplateName("payment-success");
                request.setOrderId(order.getUuid());
                request.setVariables(variables);
                emailService.sendGeneralTemplateEmail(request);
                return ResendResultDto.builder().orderNo(orderNo).success(true)
                        .message("ส่งทดสอบไปที่ " + testRecipient.trim()).build();
            }

            paymentWebhookService.sendSuccessEmail(order, txnId);
            String to = order.getCreatedBy() != null ? order.getCreatedBy().getEmail() : "";
            return ResendResultDto.builder().orderNo(orderNo).success(true).message("ส่งไปที่ " + to).build();
        } catch (ResponseStatusException e) {
            return ResendResultDto.builder().orderNo(orderNo).success(false).message(e.getReason()).build();
        } catch (Exception e) {
            log.error("[resendConfirmation] {} failed: {}", orderNo, e.getMessage());
            return ResendResultDto.builder().orderNo(orderNo).success(false).message(e.getMessage()).build();
        }
    }

    public List<ResendResultDto> resendBulk(List<String> orderNos) {
        List<ResendResultDto> results = new ArrayList<>();
        if (orderNos != null) {
            for (String orderNo : orderNos) {
                results.add(resendConfirmation(orderNo, null));
            }
        }
        return results;
    }

    private Orders loadSuccessOrder(String orderNo) {
        Orders order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderNo));
        if (!PaymentStatus.SUCCESS.toString().equalsIgnoreCase(order.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order is not SUCCESS (current: " + order.getPaymentStatus() + ")");
        }
        return order;
    }

    private String txnIdOf(Orders order) {
        return notBlank(order.getScbTransactionId()) ? order.getScbTransactionId() : order.getOrderNo();
    }

    private String eventName(Orders order) {
        return (order.getEvent() != null && order.getEvent().getName() != null)
                ? order.getEvent().getName().trim()
                : "";
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return ((Number) value).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
