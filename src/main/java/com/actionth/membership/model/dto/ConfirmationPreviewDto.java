package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationPreviewDto {

    private String orderNo;
    private String recipientTo;
    private String subject;
    private String html;
}
