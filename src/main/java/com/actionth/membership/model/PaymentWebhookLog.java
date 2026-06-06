package com.actionth.membership.model;

import javax.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "paymentWebhookLog")
public class PaymentWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of log entry: WEBHOOK (normal inbound log) or ANOMALY (unexpected/duplicate/error)
     */
    @Column(nullable = false)
    private String logType;

    @Column(nullable = false)
    private String paymentProvider;

    @Column(columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column
    private String transactionId;

    @Column
    private String orderNo;

    @Column
    private Integer orderId;

    @Column
    private String paymentStatusAtWebhookTime;

    @Column
    private String reasonType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double amount;

    @Column
    private String currency;

    @Column(nullable = false)
    private OffsetDateTime receivedDateTime;
}
