package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One garment a runner picked: the race shirt or an extra (finisher / special) one. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailShirtDto {
    private String id;
    /** RACE / FINISHER / SPECIAL */
    private String category;
    private String shirtTypeId;
    private String shirtTypeName;
    private String shirtSizeId;
    private String shirtSizeName;
}
