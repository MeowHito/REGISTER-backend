package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SliderDTO {

    private String id;

    private String descriptionEn;

    private String descriptionTh;

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private String imagePreviewUrl;

    private String alignment = "text-center";

    @NotNull(message = "Position is required")
    @Min(value = 0, message = "Position must be greater than or equal to 0")
    private Integer position;

    private Boolean active = true;
}
