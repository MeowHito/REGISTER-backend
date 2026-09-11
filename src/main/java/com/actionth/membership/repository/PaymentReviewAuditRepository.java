package com.actionth.membership.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.PaymentReviewAudit;

@Repository
public interface PaymentReviewAuditRepository extends JpaRepository<PaymentReviewAudit, Long> {

    List<PaymentReviewAudit> findByOrderNoOrderByIdDesc(String orderNo);
}
