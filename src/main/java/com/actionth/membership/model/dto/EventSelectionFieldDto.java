package com.actionth.membership.model.dto;

import java.util.List;

import com.actionth.membership.constant.SelectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSelectionFieldDto {
    private String id;
    private String title;
    private String titleEn;
    private SelectionType type;
    private boolean required;
    /** Sponsor questionnaire group (EventQuestionSectionDto.id) or null. */
    private String sectionId;
    private Integer position;
    private List<EventSelectionOptionDto> options;
}
