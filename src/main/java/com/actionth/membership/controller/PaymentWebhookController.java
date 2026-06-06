package com.actionth.membership.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.actionth.membership.model.Orders;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.service.PaymentWebhookService;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
public class PaymentWebhookController {

    @Autowired
    private PaymentWebhookService webhookService;
    
    @Autowired
    private OrderRepository orderRepository;

    /**
     * รับ webhook จาก SCB
     */
    @PostMapping("/scb")
    public ResponseEntity<Map<String, Object>> receiveSCBCallback(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> response = webhookService.processSCBWebhook(payload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing SCB webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("resCode", "99", "resDesc", "internal error"));
        }
    }

    @GetMapping("/scb/get-payload")
    public ResponseEntity<List<String>> getPayloadsByOrderNo(@RequestParam String orderNo) {
        String key = orderNo == null ? null : orderNo.trim();
        if (key == null || key.isBlank())
            return ResponseEntity.badRequest().build();

        List<String> payloads = webhookService.getPayloadsByOrderKey(key);
        
        return payloads.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(payloads);
    }

    /**
     * รับ webhook จาก 2c2p
     */
    @PostMapping("/partner-2c2p")
    public ResponseEntity<?> receive2C2PCallback(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> response = webhookService.process2C2PWebhook(body);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("[2C2P] Invalid request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[2C2P] unexpected error while processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test Endpoint for Email
     */
    @GetMapping("/test-email")
    public ResponseEntity<?> testSendSuccessEmail(@RequestParam String orderNo, @RequestParam(required = false) String transactionId) {
        Orders order = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found: " + orderNo);
        }

        try {
            String txnId = transactionId != null ? transactionId : "TEST-" + System.currentTimeMillis();
            webhookService.sendSuccessEmail(order, txnId);
            return ResponseEntity.ok("Email sent successfully for order: " + orderNo);
        } catch (Exception e) {
            log.error("Test email failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed: " + e.getMessage());
        }
    }
}
