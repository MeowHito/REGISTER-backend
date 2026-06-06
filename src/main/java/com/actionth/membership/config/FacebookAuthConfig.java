package com.actionth.membership.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FacebookAuthConfig {
    
    @Value("${login.auth.facebook.client-id}")
    private String clientId;

    @Value("${login.auth.facebook.client-secret}")
    private String clientSecret;

    @Value("${login.auth.facebook.debug-url}")
    private String debugUrl;

    @Value("${login.auth.facebook.verify-url}")
    private String verifyUrl;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getDebugUrl() {
        return debugUrl;
    }

    public String getVerifyUrl() {
        return verifyUrl;
    }

}
