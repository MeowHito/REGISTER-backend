package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SliderReorderDTO {

    @NotNull(message = "Sliders list is required")
    private List<SliderPositionDTO> sliders;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SliderPositionDTO {
        @NotNull(message = "Slider ID is required")
        private String id;

        @NotNull(message = "Position is required")
        private Integer position;
    }
}
