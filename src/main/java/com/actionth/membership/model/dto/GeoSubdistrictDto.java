package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoSubdistrictDto {
    private Integer code;
    private Integer districtCode;
    private String nameEn;
    private String nameTh;
    private String postalCode;
}
