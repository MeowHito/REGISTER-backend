package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResetPasswordDTORequest {
    private String id;
    private String role;
    private String npw;
}
