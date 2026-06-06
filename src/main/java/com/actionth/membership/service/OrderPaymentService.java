package com.actionth.membership.service;

import com.actionth.membership.model.Orders;
import com.actionth.membership.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrderPaymentService {

    @Autowired
    private OrderRepository orderRepository;

    // ดึงรายการที่ค้างชำระ
    public List<Orders> getOverduePayments() {
        return orderRepository.findOverduePayments(OffsetDateTime.now());
    }

    // อัปเดตรายการที่ค้างชำระให้เป็น 'Failed'
    @Transactional
    public int updateOverduePayments() {
        return orderRepository.updateOverduePayments(OffsetDateTime.now());
    }
}
