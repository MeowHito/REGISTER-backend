package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgeGroupDTORequest {

    private String uuid;
    private Integer eventTypeId;
    private String gender;
    private Double minAge;
    private Double maxAge;
}
