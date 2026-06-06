package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroupDto {
    private String id;
    private String gender;
    private Integer minAge;
    private Integer maxAge;
    private Integer position;
}
