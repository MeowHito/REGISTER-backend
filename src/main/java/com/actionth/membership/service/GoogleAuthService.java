package com.actionth.membership.service;

import com.actionth.membership.config.GoogleAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleAuthService {

    @Autowired
    private GoogleAuthConfig googleAuthConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> exchangeCodeForToken(String code) {
        String url = googleAuthConfig.getTokenUrl();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleAuthConfig.getClientId());
        body.add("client_secret", googleAuthConfig.getClientSecret());
        body.add("redirect_uri", googleAuthConfig.getRedirectUri());
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(response.getBody(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
            } else {
                throw new RuntimeException(
                        "Failed to exchange code for token: HTTP Status " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error exchanging code for token: " + e.getMessage(), e);
        }
    }
}
