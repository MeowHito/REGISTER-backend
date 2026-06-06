package com.actionth.membership.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "emailQueue")
@EqualsAndHashCode(callSuper = true)
public class EmailQueue extends StandardFields {

    /** Email type: CORRECTION, NOTIFICATION, CONFIRMATION, etc. */
    @Column(nullable = false, length = 30)
    private String type;

    /** Optional link to an order (nullable for non-order emails) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Orders order;

    /** Optional link to the original EmailLog (used for RETRY type) */
    @Column
    private Long emailLogId;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(length = 500)
    private String subject;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSING, SENT, FAILED

    @Column
    @Builder.Default
    private Integer retryCount = 0;

    @Column
    private OffsetDateTime scheduledAt;

    @Column
    private OffsetDateTime processedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
