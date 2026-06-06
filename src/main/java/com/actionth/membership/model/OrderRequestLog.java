package com.actionth.membership.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.OffsetDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "orderRequestLog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OrderRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    @Builder.Default
    private String requestType = "CREATE";

    @Column(length = 50)
    private String correlationId;

    @Column(length = 20)
    private String orderNo;

    @Column(length = 36)
    private String eventId;

    private Integer detailsCount;

    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Column(length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 50)
    private String errorCode;

    private Long processingTimeMs;

    @Column(length = 50)
    private String clientIp;

    @CreatedDate
    @Column(updatable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdTime;
}
