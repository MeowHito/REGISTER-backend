package com.actionth.membership.model.request;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShirtTypeDTORequest {

    private String uuid;
    private Integer eventTypeId;

    private String name;
    private String description;
    private List<ShirtSizeDTORequest> shirtSizes = new ArrayList<>();

}
