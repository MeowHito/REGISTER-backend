package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegisterSocialDTORequest {

    private String email;
    private String firstName;
    private String lastName;
    private String role;

}
