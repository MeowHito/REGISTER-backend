package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String id;
    private String paymentMethod;
    private String paymentStatus;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime paymentDueDatetime;
    private String orderNo;
    private String refNo2;
    private String refNo3;
    private Double unitPrice;
    private Double shippingFee;
    private String paymentToken;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime tokenExpireAt;
    private Integer qty;
    private Double discountShirt;
    private String coupon;
    private Double couponDiscount;
    private Double fee;
    private Double feePercent;
    private Double totalAmountWithFee;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime paymentDateTime;

    private String scbTransactionId;

    private Double totalPrice;
    private String eventId;

    @Builder.Default
    private List<OrderDetailDto> orderDetails = new ArrayList<>();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime cancelledDateTime;
    private String cancelledBy;
}