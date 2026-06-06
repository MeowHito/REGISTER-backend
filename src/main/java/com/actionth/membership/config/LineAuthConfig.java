package com.actionth.membership.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LineAuthConfig {

    @Value("${login.auth.line.client-id}")
    private String clientId;

    @Value("${login.auth.line.client-secret}")
    private String clientSecret;

    @Value("${login.auth.line.verify-url}")
    private String verifyUrl;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getVerifyUrl() {
        return verifyUrl;
    }
    
}
