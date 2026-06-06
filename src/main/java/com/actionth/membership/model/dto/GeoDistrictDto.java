package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoDistrictDto {
    private Integer code;
    private Integer provinceCode;
    private String nameEn;
    private String nameTh;
}
