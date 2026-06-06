package com.actionth.membership.model.request;

import lombok.Data;

@Data
public class OrderCancelRequest {
    private String orderId;
    private String cancelledBy;
}
