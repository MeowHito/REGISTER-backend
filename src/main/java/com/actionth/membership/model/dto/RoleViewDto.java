package com.actionth.membership.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleViewDto {
    private String roleType;
    private String role;
    private Boolean active;

    private List<PermissionViewDto> permissions; 
}
