package com.actionth.membership.model.request;

import lombok.Data;

@Data
public class UpdatePaymentMethodRequest {
    private Integer orderId;
    private String paymentMethod;
}
