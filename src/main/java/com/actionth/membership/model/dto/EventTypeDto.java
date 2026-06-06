package com.actionth.membership.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTypeDto {
    private String id;
    private String name;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    private Integer quota;
    private BigDecimal price;
    private Boolean isNoShirt;
    private BigDecimal discountNoShirt;

    private List<PricingDto> pricing;
    private List<AgeGroupDto> ageGroups;
    private List<EventSelectionFieldDto> selectionFields;

    private Boolean isTeam;
    private Boolean isQuotaFull;
}
