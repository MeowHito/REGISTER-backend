package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;

import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One add-on purchased on an order. Name and unit price are snapshotted at
 * purchase time so a later edit to the organizer's {@link EventAddOn} never
 * rewrites what somebody already paid.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "orderAddOn")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class OrderAddOn extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("order-orderAddOn")
    @ToString.Exclude
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addOnId")
    @ToString.Exclude
    private EventAddOn addOn;

    /** Set only for a per-applicant add-on: which runner it belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderDetailId")
    @ToString.Exclude
    private OrderDetail orderDetail;

    private String name;
    private String nameEn;

    private Double unitPrice;
    private Integer qty;
    private Double totalPrice;

    @Column(length = 500)
    private String note;
}
