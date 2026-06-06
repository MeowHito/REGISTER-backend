package com.actionth.membership.model.request;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendBibRequest {

    @NotBlank(message = "ต้องระบุ event ID")
    private String id;
}
