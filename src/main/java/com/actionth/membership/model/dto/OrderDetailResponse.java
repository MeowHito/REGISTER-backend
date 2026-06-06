package com.actionth.membership.model.dto;



import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Data
public class OrderDetailResponse {
    private String orderNo;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdTime;
    private List<OrderItemDto> items;

    @Data
    public static class OrderItemDto {
        private String fullName;
        private String eventTypeName;
        private Double price;
    }
}
