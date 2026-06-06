package com.actionth.membership.service;

import com.actionth.membership.config.LineAuthConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.actionth.membership.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class LineAuthService {

    @Autowired
    private LineAuthConfig lineAuthConfig;

    @Autowired
    private RestTemplate restTemplate;

    public String verifyIdToken(String idToken) {
        String url = lineAuthConfig.getVerifyUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
        body.add("id_token", idToken);
        body.add("client_id", lineAuthConfig.getClientId());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new BusinessException("Failed to verify ID token: HTTP Status " + response.getStatusCode());
        }
    }
}
