package com.actionth.membership.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * mPAY (AIS) payment gateway — "mPAY ONE API Specification" v4.19.
 *
 * While {@code enabled} is false every order still goes through SCB / 2C2P, so the code can
 * ship before the merchant credentials arrive. Each channel needs its own {@code service_id}
 * from mPAY; a channel whose service id is blank is not offered to runners.
 */
@Configuration
@Getter
public class MPayConfig {

    @Value("${mpay.enabled:false}")
    private boolean enabled;

    /** e.g. https://api.stg-paymentgateway.ais.co.th/stg (mPAY ONE) or the Modernization host. */
    @Value("${mpay.base-url:}")
    private String baseUrl;

    @Value("${mpay.merchant-id:}")
    private String merchantId;

    @Value("${mpay.channel-secret:}")
    private String channelSecret;

    /** The spec's sample code does not show the hex alphabet; flip this if mPAY rejects lowercase. */
    @Value("${mpay.signature-uppercase:false}")
    private boolean signatureUppercase;

    @Value("${mpay.service-id.creditcard:}")
    private String serviceIdCreditCard;

    @Value("${mpay.service-id.qrcode:}")
    private String serviceIdQr;

    @Value("${mpay.service-id.mobilebanking:}")
    private String serviceIdMobileBanking;

    @Value("${mpay.service-id.linepay:}")
    private String serviceIdLinePay;

    @Value("${mpay.service-id.shopeepay:}")
    private String serviceIdShopeePay;

    @Value("${mpay.service-id.paotang:}")
    private String serviceIdPaotang;

    /** Bank codes offered for Internet/Mobile Banking (spec §11). */
    @Value("${mpay.banks:002,004,006,011,014,025}")
    private String banks;

    @Value("${mpay.qr-expire-seconds:600}")
    private long qrExpireSeconds;

    @Value("${app.fe-url}")
    private String feUrl;

    @Value("${app.be-url}")
    private String beUrl;

    public String serviceIdFor(String paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        String id = switch (paymentMethod) {
            case "creditcard" -> serviceIdCreditCard;
            case "qrcode" -> serviceIdQr;
            case "mobilebanking" -> serviceIdMobileBanking;
            case "linepay" -> serviceIdLinePay;
            case "shopeepay" -> serviceIdShopeePay;
            case "paotang" -> serviceIdPaotang;
            default -> null;
        };
        return id == null || id.isBlank() ? null : id.trim();
    }

    public List<String> bankCodes() {
        return Arrays.stream(banks.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** True once the gateway can actually be called. */
    public boolean isActive() {
        return enabled && !isBlank(baseUrl) && !isBlank(merchantId) && !isBlank(channelSecret);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
