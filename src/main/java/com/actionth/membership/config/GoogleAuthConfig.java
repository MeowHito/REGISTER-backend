package com.actionth.membership.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAuthConfig {
    
    @Value("${login.auth.google.client-id}")
    private String clientId;

    @Value("${login.auth.google.client-secret}")
    private String clientSecret;

    @Value("${login.auth.google.token-url}")
    private String tokenUrl;

    @Value("${login.auth.google.redirect-uri}")
    private String redirectUri;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

}
