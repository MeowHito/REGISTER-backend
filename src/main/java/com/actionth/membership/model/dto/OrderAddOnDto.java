package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAddOnDto {
    private String id;
    private String addOnId;
    /** uuid of the runner this add-on belongs to; null for a per-order add-on. */
    private String orderDetailId;
    private String applicantName;
    private String name;
    private String nameEn;
    /** The organizer's label for {@link #note}, e.g. "วันเช็คอิน". */
    private String noteLabel;
    private Double unitPrice;
    private Integer qty;
    private Double totalPrice;
    private String note;
}
