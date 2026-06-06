package com.actionth.membership.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ข้อมูลขนาดเสื้อที่ถูกแมปกับประเภทเสื้อ")
public class ShirtSizeMappingDTO {

    @Schema(description = "ID ของการแมปขนาดเสื้อ", example = "1")
    private Integer mappingId;

    @Schema(description = "ID ของขนาดเสื้อ", example = "10")
    private Integer sizeId;

    @Schema(description = "ชื่อขนาดเสื้อ", example = "L")
    private String sizeName;

    @Schema(description = "รายละเอียดขนาดเสื้อ", example = "รอบอก 42 นิ้ว")
    private String sizeDescription;

    @Schema(description = "วันที่สร้าง", example = "2025-02-04T00:42:27.111Z")
    private OffsetDateTime createdTime;

    @Schema(description = "สถานะ Active", example = "true")
    private Boolean active;
}
