package com.actionth.membership.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAddOnDto {
    private String id;
    private String name;
    private String nameEn;
    private String description;
    private String descriptionEn;
    private String category;
    private String imageUrl;
    private String prefixPath;
    private BigDecimal price;
    private Integer quota;
    private Integer maxPerOrder;
    private Boolean perApplicant;
    private String noteLabel;
    private String noteLabelEn;
    private Boolean noteRequired;
    private Integer position;

    /** false = hidden from the registration page but kept for existing orders. */
    private Boolean active;

    /** Units already taken by SUCCESS/PENDING/REVIEW orders. Read-only. */
    private Integer usedQuota;
    /** Units still on sale, or {@code null} when the add-on has no quota. */
    private Integer availableQuota;
    private Boolean isSoldOut;
}
