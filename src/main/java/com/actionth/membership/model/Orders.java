package com.actionth.membership.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.List;
import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "orders")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(callSuper = true)
public class Orders extends StandardFields {

    private String paymentMethod;
    private String paymentStatus;
    private OffsetDateTime paymentDueDatetime;
    @Column(unique = true)
    private String orderNo;
    private String refNo2;
    private String refNo3;
    private Double unitPrice;
    private Double shippingFee;
    private String paymentToken;
    private OffsetDateTime tokenExpireAt;
    private Integer qty;
    private Double discountShirt;
    private String coupon;
    private Double couponDiscount;
    private Double fee;
    private Double feePercent;
    private Double totalAmountWithFee;
    private OffsetDateTime paymentDateTime;

    @Column(length = 100)
    private String scbTransactionId;

    private String reviewReason;

    @Builder.Default
    private Double totalPrice = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventId", referencedColumnName = "id", nullable = false)
    private Event event;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("order-orderDetail")
    @Builder.Default
    private List<OrderDetail> orderDetails = new ArrayList<>();

    private OffsetDateTime cancelledDateTime;
    private String cancelledBy;

    @Builder.Default
    @Column(name = "correctionEmailSent")
    private Boolean correctionEmailSent = false;
}
