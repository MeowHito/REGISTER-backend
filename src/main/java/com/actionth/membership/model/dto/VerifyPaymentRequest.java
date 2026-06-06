package com.actionth.membership.model.dto;

import lombok.Data;

@Data
public class VerifyPaymentRequest {
    /**
     * Payment type — must match one of the frontend payment options:
     * <ul>
     *   <li>{@code QR_CODE}       — สแกนจ่าย/โอนเงิน (PromptPay QR) → SCB slip verify</li>
     *   <li>{@code CREDIT_CARD}   — บัตรเดบิต/เครดิต → 2C2P inquiry</li>
     *   <li>{@code LINE_TRUEMONEY}— อีวอลเล็ท (LINE Pay, TrueMoney) → 2C2P inquiry</li>
     *   <li>{@code ALIPAY}        — Alipay+ → SCB eWallet inquiry</li>
     *   <li>{@code WECHAT_PAY}    — WeChatPay → SCB eWallet inquiry</li>
     *   <li>{@code LOG_SETTLE}    — (internal) settle from webhook logs</li>
     * </ul>
     */
    private String paymentType;

    // ALIPAY / WECHAT_PAY
    private String tranType;
    private String outTradeNo;

    // QR_CODE (slip verify)
    private String slipQrRawData;

    // CREDIT_CARD / LINE_TRUEMONEY (2C2P)
    private String invoiceNo;

    // QR_CODE / LOG_SETTLE
    private String orderNo;
}
