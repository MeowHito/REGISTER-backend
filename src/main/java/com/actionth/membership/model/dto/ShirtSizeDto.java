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
public class ShirtSizeDto {
    private String id;
    private String name;
    private BigDecimal chestSize;
    private BigDecimal lengthSize;
    private Integer position;
    /** Runners (paid or holding a slot) who picked this size; read-only, for the back office. */
    private Long usedCount;
}
