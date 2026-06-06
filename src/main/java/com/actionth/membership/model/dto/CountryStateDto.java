package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryStateDto {
    private String id;
    private String countryEn;
    private String countryLocal;
    private String stateEn;
    private String stateLocal;
    private String stateType;
}
