package com.actionth.membership.model.request;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderRequest {
    private String paymentMethod;
    private OffsetDateTime paymentDueDatetime;
    private String refno2;
    private String refno3;
    private Double unitPrice;
    private Double shippingFee;
    private Double totalPrice;
    private String eventId;
    private Integer qty;
    private Double discountShirt;
    private String coupon;
    private Double couponDiscount;
    private Double fee;
    private Double feePercent;
    private Double totalAmountWithFee;

    private List<OrderDetailRequest> orderDetails;
}
