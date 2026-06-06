package com.actionth.membership.controller;

import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.OrderUpdateResponse;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.service.OrderService;
import com.actionth.membership.service.PaymentGatewayService;
import com.actionth.membership.service.PaymentWebhookService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.actionth.membership.model.dto.OrderDto;
import com.actionth.membership.model.request.OrderRequest;
import com.actionth.membership.model.request.OrderUpdateRequest;
import com.actionth.membership.model.request.UpdatePaymentMethodRequest;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentWebhookService paymentWebhookService;

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/create")
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderRequest orderRequest) {
        OrderDto createdOrder = orderService.createOrder(orderRequest);
        return ResponseEntity.ok(createdOrder);
    }

    @GetMapping("/validate-token/{token}")
    public ResponseEntity<Map<String, Object>> validateToken(@PathVariable String token) {
        Orders order = orderService.findByToken(token);
        Map<String, Object> response = new HashMap<>();

        if (order == null) {
            response.put("status", "not_found");
            response.put("message", "Token ไม่ถูกต้อง");
        } else if (order.getTokenExpireAt().isBefore(OffsetDateTime.now())) {
            response.put("status", "expired");
            response.put("message", "Token หมดอายุแล้ว");
        } else {
            response.put("status", "valid");
            response.put("message", "Token ใช้งานได้");
            response.put("orderId", order.getId());
            response.put("orderNo", order.getOrderNo());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-payment-method")
    public ResponseEntity<Map<String, String>> updatePaymentMethod(@RequestBody UpdatePaymentMethodRequest request) {
        boolean updated = orderService.updatePaymentMethod(request.getOrderId(), request.getPaymentMethod());

        Map<String, String> response = new HashMap<>();
        if (updated) {
            response.put("status", "success");
            response.put("message", "อัปเดตวิธีชำระเงินสำเร็จ");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "ไม่พบ order ที่ต้องการอัปเดต");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<OrderUpdateResponse> updatePayment(@RequestBody OrderUpdateRequest request) {
        log.info("Received payment update request for orderId: {}", request.getOrderId());
        OrderUpdateResponse response = orderService.updateOrderPayment(request);

        if (response.isSkipPayment()) {
            log.info("Order fully discounted (skipPayment=true), sending success email for orderId: {}", request.getOrderId());
            try {
                Orders order = orderService.findByUuid(request.getOrderId());
                if (order != null) {
                    paymentWebhookService.sendSuccessEmail(order, "FULLY_DISCOUNTED");
                }
            } catch (Exception e) {
                log.warn("Failed to send success email for fully discounted order orderId={}: {}", request.getOrderId(), e.getMessage());
            }
        } else {
            try {
                Orders order = orderService.findByUuid(request.getOrderId());
                if (order != null && order.getTotalAmountWithFee() != null && order.getTotalAmountWithFee() > 0) {
                    if (!"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
                        log.warn("Order status is not PENDING for orderId={}, status={}", request.getOrderId(), order.getPaymentStatus());
                        response.setMessage("ORDER_STATUS_NOT_PENDING");
                        return ResponseEntity.ok(response);
                    }
                    OffsetDateTime due = order.getPaymentDueDatetime();
                    if (due != null && due.isBefore(OffsetDateTime.now())) {
                        log.warn("Order is overdue for orderId={}", request.getOrderId());
                        response.setMessage("ORDER_OVERDUE");
                        return ResponseEntity.ok(response);
                    }

                    OrderUpdateResponse.PaymentData paymentData = paymentGatewayService.initiatePayment(order);
                    response.setPaymentData(paymentData);
                    log.info("Payment initiated successfully for orderId: {}, method: {}", request.getOrderId(), request.getPaymentMethod());
                }
            } catch (Exception e) {
                log.error("Failed to initiate payment for orderId={}: {}", request.getOrderId(), e.getMessage(), e);
                response.setMessage("Order updated but payment initiation failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Resend payment success email with timezone clarification
     * Sends the same confirmation email but with a correction notice explaining timezone display
     */
    @PostMapping("/{orderUuid}/resend-payment-email")
    public ResponseEntity<Map<String, Object>> resendPaymentEmail(@PathVariable String orderUuid) {
        log.info("[Resend Email API] Received request to resend payment email for orderUuid: {}", orderUuid);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Find the order
            Orders order = orderService.findByUuid(orderUuid);
            
            if (order == null) {
                log.warn("[Resend Email API] Order not found for uuid: {}", orderUuid);
                response.put("success", false);
                response.put("message", "ไม่พบคำสั่งซื้อที่ระบุ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify payment status
            if (!"SUCCESS".equals(order.getPaymentStatus())) {
                log.warn("[Resend Email API] Cannot resend - order payment status is {}", order.getPaymentStatus());
                response.put("success", false);
                response.put("message", "ไม่สามารถส่งอีเมลสำหรับคำสั่งซื้อที่ยังไม่ชำระเงินได้");
                return ResponseEntity.badRequest().body(response);
            }

            // Only resend for upcoming events (eventDate > now)
            if (order.getEvent() != null && order.getEvent().getEventDate() != null
                    && order.getEvent().getEventDate().isBefore(OffsetDateTime.now())) {
                log.warn("[Resend Email API] Cannot resend - event date {} has already passed", order.getEvent().getEventDate());
                response.put("success", false);
                response.put("message", "ไม่สามารถส่งอีเมลสำหรับกิจกรรมที่ผ่านไปแล้วได้");
                return ResponseEntity.badRequest().body(response);
            }
            
            // TODO: Add user authorization check here if needed
            // Currently allows any authenticated user to resend for any order
            // Should verify: user owns the order OR user has ADMIN role
            
            // Resend email with correction notice
            paymentWebhookService.resendSuccessEmailWithCorrection(order);
            
            log.info("[Resend Email API] Successfully resent email for orderNo={}, orderUuid={}", 
                    order.getOrderNo(), orderUuid);
            
            response.put("success", true);
            response.put("message", "ส่งอีเมลยืนยันพร้อมคำชี้แจงเรื่องเวลาสำเร็จแล้ว");
            response.put("orderNo", order.getOrderNo());
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.error("[Resend Email API] Validation error for orderUuid={}: {}", orderUuid, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("[Resend Email API] Failed to resend email for orderUuid={}: {}", 
                    orderUuid, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "เกิดข้อผิดพลาดในการส่งอีเมล: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Batch resend payment success emails with timezone correction notice for all paid orders of an event.
     * Useful for correcting timezone display for all customers who received emails before the fix.
     */
    @PostMapping("/batch-resend-payment-email/{eventUuid}")
    public ResponseEntity<Map<String, Object>> batchResendPaymentEmail(@PathVariable String eventUuid) {
        log.info("[Batch Resend] Received request for eventUuid: {}", eventUuid);

        Map<String, Object> response = new HashMap<>();

        try {
            List<Orders> successOrders = orderRepository.findSuccessOrdersByEventUuid(eventUuid);

            if (successOrders.isEmpty()) {
                response.put("success", false);
                response.put("message", "ไม่พบคำสั่งซื้อที่ชำระเงินแล้วสำหรับกิจกรรมนี้");
                response.put("totalOrders", 0);
                return ResponseEntity.ok(response);
            }

            int sentCount = 0;
            List<String> failedOrderNos = new ArrayList<>();

            for (Orders order : successOrders) {
                try {
                    paymentWebhookService.resendSuccessEmailWithCorrection(order);
                    sentCount++;
                    log.info("[Batch Resend] Sent for orderNo={}", order.getOrderNo());
                } catch (Exception e) {
                    log.error("[Batch Resend] Failed for orderNo={}: {}", order.getOrderNo(), e.getMessage());
                    failedOrderNos.add(order.getOrderNo());
                }
            }

            response.put("success", true);
            response.put("message", String.format("ส่งอีเมลชี้แจงเรื่องเวลาสำเร็จ %d/%d รายการ", sentCount, successOrders.size()));
            response.put("totalOrders", successOrders.size());
            response.put("sentCount", sentCount);
            response.put("failedCount", failedOrderNos.size());
            if (!failedOrderNos.isEmpty()) {
                response.put("failedOrderNos", failedOrderNos);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Batch Resend] Failed for eventUuid={}: {}", eventUuid, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "เกิดข้อผิดพลาด: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Resend payment success emails with timezone correction for ALL paid orders across all events.
     */
    @PostMapping("/resend-all-payment-email")
    public ResponseEntity<Map<String, Object>> resendAllPaymentEmail() {
        log.info("[Resend All] Received request to resend payment emails for all paid orders");

        Map<String, Object> response = new HashMap<>();

        try {
            List<Orders> successOrders = orderRepository.findAllSuccessOrders();

            if (successOrders.isEmpty()) {
                response.put("success", false);
                response.put("message", "ไม่พบคำสั่งซื้อที่ชำระเงินแล้ว");
                response.put("totalOrders", 0);
                return ResponseEntity.ok(response);
            }

            int sentCount = 0;
            List<String> failedOrderNos = new ArrayList<>();

            for (Orders order : successOrders) {
                try {
                    paymentWebhookService.resendSuccessEmailWithCorrection(order);
                    sentCount++;
                    log.info("[Resend All] Sent for orderNo={}", order.getOrderNo());
                } catch (Exception e) {
                    log.error("[Resend All] Failed for orderNo={}: {}", order.getOrderNo(), e.getMessage());
                    failedOrderNos.add(order.getOrderNo());
                }
            }

            response.put("success", true);
            response.put("message", String.format("ส่งอีเมลชี้แจงเรื่องเวลาสำเร็จ %d/%d รายการ", sentCount, successOrders.size()));
            response.put("totalOrders", successOrders.size());
            response.put("sentCount", sentCount);
            response.put("failedCount", failedOrderNos.size());
            if (!failedOrderNos.isEmpty()) {
                response.put("failedOrderNos", failedOrderNos);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Resend All] Failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "เกิดข้อผิดพลาด: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

}
