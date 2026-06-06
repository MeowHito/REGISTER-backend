package com.actionth.membership.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Value("${scb.app-key}")
    private String appKey;

    @Value("${scb.app-secret}")
    private String appSecret;

    @Value("${scb.base-url}")
    private String baseUrl;

    @Value("${scb.company-id}")
    private String companyId;

    @Value("${scb.terminal-id}")
    private String terminalId;

    @Value("${scb.pp-id}")
    private String ppId;

    @Value("${scb.ref3}")
    private String ref3;

    @Value("${2c2p.merchant-id}")
    private String merchantId2C2P;

    @Value("${2c2p.merchant-id-ewallet}")
    private String merchantIdEwallet2C2P;

    @Value("${2c2p.secret-key}")
    private String secretKey2C2P;

    @Value("${2c2p.inquiry-url}")
    private String inquiryUrl2C2P;

    @Value("${2c2p.token-url}")
    private String tokenUrl2C2P;

    @Value("${2c2p.frontend-return-url}")
    private String frontendReturnUrl2C2P;

    @Value("${2c2p.backend-return-url}")
    private String backendReturnUrl2C2P;

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getMerchantId2C2P() {
        return merchantId2C2P;
    }

    public String getMerchantIdEwallet2C2P() {
        return merchantIdEwallet2C2P;
    }

    public String getSecretKey2C2P() {
        return secretKey2C2P;
    }

    public String getInquiryUrl2C2P() {
        return inquiryUrl2C2P;
    }

    public String getPpId() {
        return ppId;
    }

    public String getRef3() {
        return ref3;
    }

    public String getTokenUrl2C2P() {
        return tokenUrl2C2P;
    }

    public String getFrontendReturnUrl2C2P() {
        return frontendReturnUrl2C2P;
    }

    public String getBackendReturnUrl2C2P() {
        return backendReturnUrl2C2P;
    }
}
