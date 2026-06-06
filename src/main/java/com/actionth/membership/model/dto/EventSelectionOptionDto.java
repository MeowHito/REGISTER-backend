package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSelectionOptionDto {
    private String id;
    private String value;
    private String valueEn;
    private String inputType;
    private int position;
}
