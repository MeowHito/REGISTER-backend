package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.OffsetDateTime;

/** One bell-icon notification for one back-office user. */
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification", indexes = {
        @Index(name = "IDX_notification_recipient_read", columnList = "recipientId, readAt")
})
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class Notification extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipientId", nullable = false)
    @ToString.Exclude
    private User recipient;

    /** See {@link com.actionth.membership.constant.NotificationType}. */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String message;

    /** Back-office path to open when clicked, e.g. "/backoffice/helpRequests". */
    @Column(length = 500)
    private String link;

    private OffsetDateTime readAt;
}
