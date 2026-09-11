package com.actionth.membership.model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResendBulkRequest {

    private List<String> orderNos;
}
