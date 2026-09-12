package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * An optional package an organizer sells on top of the race entry —
 * accommodation, a photo package, a shuttle seat, extra merchandise…
 *
 * The organizer decides how each one is bought:
 * - {@code perApplicant = false} → bought once for the whole order with a
 *   quantity (2 hotel rooms for a group of four).
 * - {@code perApplicant = true}  → ticked per runner (a photo package each),
 *   so the booking is attached to that runner's {@link OrderDetail}.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "eventAddOn")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class EventAddOn extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("event-addOns")
    @ToString.Exclude
    private Event event;

    private String name;
    private String nameEn;

    /** Free-form details the organizer writes (rich text from the tiptap editor). */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String descriptionEn;

    /** Optional grouping shown as a heading, e.g. "ที่พัก" / "Accommodation". */
    private String category;

    private String imageUrl;
    private String prefixPath;

    private BigDecimal price;

    /** Total units on sale. {@code null} = unlimited. */
    private Integer quota;

    /** Cap per order for a per-order add-on. {@code null} = no cap. */
    private Integer maxPerOrder;

    /** false = one purchase per order with a quantity; true = ticked per runner. */
    private Boolean perApplicant;

    /** When set, the buyer gets a note box with this label (e.g. "วันเช็คอิน"). */
    private String noteLabel;
    private String noteLabelEn;
    private Boolean noteRequired;

    private Integer position;
}
