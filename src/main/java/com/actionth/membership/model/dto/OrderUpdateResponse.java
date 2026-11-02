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
        /** "MPAY" for mPAY payments; null for the legacy SCB / 2C2P flows. */
        private String provider;
        private String paymentMethod;
        /** Deep link into the wallet/bank app (mPAY LINE Pay, Paotang), when the gateway gives one. */
        private String appPaymentUrl;
        private String qrExpireAt;
        private String qrImage;
        private String webPaymentUrl;
        private String payload;
        private String respCode;
        private String outTradeNo;
        private String tranType;
        private String refNo;
    }
}
