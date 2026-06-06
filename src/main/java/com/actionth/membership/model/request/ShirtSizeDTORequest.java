package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShirtSizeDTORequest {

    private String uuid;
    private Integer shirtTypeId;

    private String name;
    private String chestSize;
    private String lengthSize;

}
