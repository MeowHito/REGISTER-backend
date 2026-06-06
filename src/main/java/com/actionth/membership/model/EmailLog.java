package com.actionth.membership.model;

import javax.persistence.*;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "emailLog")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = true)
    private String orderId;

    @Column(nullable = false)
    private String recipientTo;

    @Column
    private String recipientCc;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String emailBody;

    @Column(length = 20, nullable = false)
    private String sendStatus;

    @Column
    private Integer retryCount = 0;

    @Column
    private Boolean hasAttachments = false;

    @Column
    private Integer attachmentCount = 0;

    @Column
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdAt;

    @Column
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 500)
    private String attachmentPath;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
