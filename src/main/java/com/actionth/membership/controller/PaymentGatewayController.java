package com.actionth.membership.controller;

import com.actionth.membership.model.dto.VerifyPaymentRequest;
import com.actionth.membership.service.PaymentGatewayService;
import com.actionth.membership.service.PaymentWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/gatewaypayment")
public class PaymentGatewayController {

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    @Autowired
    private PaymentWebhookService webhookService;

    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        String type = request.getPaymentType();
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_PAYMENT_TYPE");
        }

        // ── QR Code (สแกนจ่าย/โอนเงิน) — SCB slip verify ──
        if ("QR_CODE".equalsIgnoreCase(type)) {
            if (request.getSlipQrRawData() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_SLIP_DATA");
            }
            if (request.getOrderNo() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_ORDER_NO");
            }

            Map<String, Object> result = paymentGatewayService.verifyTransaction(
                    request.getSlipQrRawData(),
                    request.getOrderNo());

            Object reason = result.get("reason");
            if (reason != null) {
                return ResponseEntity.ok(Map.of(
                        "settled", result.get("settled"),
                        "status", result.get("status"),
                        "reason", reason));
            }
            return ResponseEntity.ok(Map.of(
                    "settled", result.get("settled"),
                    "status", result.get("status")));
        }

        // ── Credit/Debit Card & LINE Pay/TrueMoney — 2C2P inquiry ──
        if ("CREDIT_CARD".equalsIgnoreCase(type) || "LINE_TRUEMONEY".equalsIgnoreCase(type)) {
            if (request.getInvoiceNo() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_INVOICE_NO");
            }
            Map<String, Object> result = paymentGatewayService.inquire2C2PPayment(request.getInvoiceNo(), type);
            return ResponseEntity.ok(result);
        }

        // ── Alipay+ / WeChatPay — SCB eWallet inquiry ──
        if ("ALIPAY".equalsIgnoreCase(type) || "WECHAT_PAY".equalsIgnoreCase(type)) {
            boolean settled = paymentGatewayService.inquireEwalletPayment(
                request.getTranType(),
                request.getOutTradeNo());
            return ResponseEntity.ok(Map.of(
                    "settled", settled,
                    "status", settled ? "SUCCESS" : "PENDING"));
        }

        // ── Internal: settle from webhook logs ──
        if ("LOG_SETTLE".equalsIgnoreCase(type)) {
            if (request.getOrderNo() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_ORDER_NO");
            }
            boolean settled = webhookService.verifyAndSettleFromLogs(request.getOrderNo());
            return ResponseEntity.ok(Map.of(
                    "settled", settled,
                    "status", settled ? "SUCCESS" : "PENDING"));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PAYMENT_TYPE");
    }

}
