package com.actionth.membership.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * A group of extra questions shown together on the registration form, typically one per
 * sponsor: it has its own title and logo, and a share token that lets the sponsor download the
 * answers as Excel without a back-office account.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "eventQuestionSection")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EventQuestionSection extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("event-questionSections")
    @ToString.Exclude
    private Event event;

    private String title;
    private String titleEn;

    @Column(length = 1000)
    private String description;

    /** Sponsor logo: object key + storage prefix, like every other image on the platform. */
    private String logoUrl;
    private String prefixPath;

    private Integer position;

    /** Random token in the sponsor's download link; regenerated on demand from the back office. */
    @Column(length = 64)
    private String shareToken;
}
