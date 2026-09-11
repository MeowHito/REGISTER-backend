package com.actionth.membership.model;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "paymentReviewAudit")
public class PaymentReviewAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private Integer adminUserId;

    private String action;

    private String outcome;

    private String transactionId;

    @Column(columnDefinition = "LONGTEXT")
    private String beforeJson;

    @Column(columnDefinition = "LONGTEXT")
    private String afterJson;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(nullable = false)
    private OffsetDateTime createdDateTime;
}
