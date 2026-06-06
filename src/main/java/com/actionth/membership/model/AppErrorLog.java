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
@Table(name = "appErrorLog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AppErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36)
    private String logId;

    /** FRONTEND or BACKEND */
    @Column(length = 20, nullable = false)
    private String source;

    @Column(length = 10, nullable = false)
    private String level;

    @Column(length = 100, nullable = false)
    private String context;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stack;

    @Column(length = 500)
    private String url;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 36)
    private String userId;

    @Column(length = 10)
    private String httpMethod;

    private Integer httpStatus;

    @Column(length = 100)
    private String statusText;

    @Column(columnDefinition = "TEXT")
    private String responseData;

    @Column(columnDefinition = "TEXT")
    private String requestData;

    @Column(columnDefinition = "TEXT")
    private String meta;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime clientTimestamp;

    @Column(length = 50)
    private String clientIp;

    @Column(length = 100)
    private String sessionId;

    @CreatedDate
    @Column(updatable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdTime;
}
