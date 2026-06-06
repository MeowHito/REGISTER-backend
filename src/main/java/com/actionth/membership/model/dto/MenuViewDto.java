package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuViewDto {
    private String title;
    private String path;
    private String icon;
    private Boolean isDisplay;
    private Boolean disabled;
    private Boolean isNoti;
    private String badgeKey;
    private Integer position;
}
