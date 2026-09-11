package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendResultDto {

    private String orderNo;
    private boolean success;
    private String message;
}
