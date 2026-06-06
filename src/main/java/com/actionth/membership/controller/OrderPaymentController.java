package com.actionth.membership.controller;

import com.actionth.membership.model.Orders;
import com.actionth.membership.service.OrderPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-payment")
public class OrderPaymentController {

    @Autowired
    private OrderPaymentService orderPaymentService;

    // API ดึงรายการที่เลยกำหนด
    @GetMapping("/overdue")
    public ResponseEntity<List<Orders>> getOverduePayments() {
        List<Orders> overdueOrders = orderPaymentService.getOverduePayments();
        return ResponseEntity.ok(overdueOrders);
    }

    // API อัปเดตรายการที่เลยกำหนดให้เป็น 'Failed'
    @PostMapping("/update-overdue")
    public ResponseEntity<String> updateOverduePayments() {
        int updatedCount = orderPaymentService.updateOverduePayments();
        return ResponseEntity.ok("อัปเดตสำเร็จ " + updatedCount + " รายการเป็น Failed");
    }
}
