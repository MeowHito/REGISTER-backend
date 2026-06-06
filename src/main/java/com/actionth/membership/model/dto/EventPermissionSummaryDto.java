package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPermissionSummaryDto {

    private String role;
    private Boolean canRead;
    private Boolean canUpdate;
    private Boolean canDelete;

}
