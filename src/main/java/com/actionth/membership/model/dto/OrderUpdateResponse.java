package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateResponse {
    private boolean skipPayment;
    private String message;
    private PaymentData paymentData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {
        private String paymentMethod;
        private String qrImage;
        private String webPaymentUrl;
        private String payload;
        private String respCode;
        private String outTradeNo;
        private String tranType;
        private String refNo;
    }
}
