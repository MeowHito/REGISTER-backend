package com.actionth.membership.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppErrorLogRequest {

    /**
     * Log level: error, warn, info
     */
    private String level;

    /**
     * Source: FRONTEND or BACKEND
     */
    private String source;

    /**
     * Context where error occurred (e.g., PaymentFlow, API_ERROR, SCB_API, 2C2P_API)
     */
    private String context;

    private String message;

    private String stack;

    private String url;

    private String userAgent;

    private String timestamp;

    private String method;

    private Integer status;

    private String statusText;

    private Object responseData;

    private String requestData;

    @Builder.Default
    private Map<String, Object> additionalFields = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalField(String key, Object value) {
        if (this.additionalFields == null) {
            this.additionalFields = new HashMap<>();
        }
        this.additionalFields.put(key, value);
    }
}
