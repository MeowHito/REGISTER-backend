package com.actionth.membership.service;

import com.actionth.membership.config.FacebookAuthConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FacebookAuthService {

    @Autowired
    private FacebookAuthConfig facebookAuthConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public boolean validateAccessToken(String accessToken) {
        try {
            String appAccessToken = facebookAuthConfig.getClientId() + "|" + facebookAuthConfig.getClientSecret();
            String debugUrl = UriComponentsBuilder.fromHttpUrl(facebookAuthConfig.getDebugUrl())
                    .queryParam("input_token", accessToken)
                    .queryParam("access_token", appAccessToken)
                    .build().toUriString();

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(debugUrl, String.class);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Invalid Facebook Access Token: " + responseEntity.getStatusCode());
            }

            JsonNode responseJson = objectMapper.readTree(responseEntity.getBody());
            JsonNode isValidNode = responseJson.path("data").path("is_valid");
            return isValidNode.asBoolean(false);
        } catch (Exception e) {
            throw new RuntimeException("Error validating Facebook access token", e);
        }
    }

    public String getEmailFromFacebook(String accessToken) {
        try {
            String userInfoUrl = UriComponentsBuilder.fromHttpUrl(facebookAuthConfig.getVerifyUrl())
                    .queryParam("fields", "email")
                    .queryParam("access_token", accessToken)
                    .build().toUriString();

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(userInfoUrl, String.class);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Failed to fetch user email from Facebook: " + responseEntity.getStatusCode());
            }

            JsonNode responseJson = objectMapper.readTree(responseEntity.getBody());
            JsonNode emailNode = responseJson.path("email");

            if (emailNode.isMissingNode() || emailNode.asText().isEmpty()) {
                throw new RuntimeException("Email not found in Facebook response.");
            }

            return emailNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching email from Facebook", e);
        }
    }
}