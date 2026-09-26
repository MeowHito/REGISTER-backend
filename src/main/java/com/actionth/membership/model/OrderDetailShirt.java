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
 * An extra garment a runner picked besides the race shirt (finisher shirt, VIP shirt...). The
 * race shirt itself stays on {@code orderDetail.shirtType/shirtSize} so nothing that reads it
 * has to change.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "orderDetailShirt")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class OrderDetailShirt extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderDetailId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("orderDetail-shirts")
    @ToString.Exclude
    private OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shirtTypeId")
    @ToString.Exclude
    private ShirtType shirtType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shirtSizeId")
    @ToString.Exclude
    private ShirtSize shirtSize;

    /** FINISHER / SPECIAL — copied from the style so the row still reads if the style is edited. */
    @Column(length = 20)
    private String category;
}
