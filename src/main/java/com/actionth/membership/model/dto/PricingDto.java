package com.actionth.membership.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingDto {
    private String id;
    private String paymentTypeId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String paymentName;
    private BigDecimal price;
    private Integer quota;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endDate;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isQuotaFull;
}
