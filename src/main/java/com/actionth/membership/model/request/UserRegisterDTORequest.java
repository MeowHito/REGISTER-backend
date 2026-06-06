package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegisterDTORequest {

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String companyName;
    private String role;

}
