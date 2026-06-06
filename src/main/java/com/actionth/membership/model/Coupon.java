package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

import javax.persistence.*;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "coupon")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(callSuper = true)
public class Coupon extends StandardFields {

    @JsonBackReference("coupon-event")
    @ManyToOne
    @JoinColumn(name = "eventId")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "redeemBy")
    private OrderDetail redeemBy;

    private String bucketName;
    private String couponName;
    private String couponCode;
    private String runnerIdNo;
    private Long deductionPercentage;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime expiryTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime redeemTime;
    private Integer limitCoupon;

    @JsonBackReference("coupon-event")
    @ManyToOne
    @JoinColumn(name = "oldEventId")
    private Event oldEvent;

    private String oldEventName;
    private String status;
    private String type;
}
