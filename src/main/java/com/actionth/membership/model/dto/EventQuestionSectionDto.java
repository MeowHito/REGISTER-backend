package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventQuestionSectionDto {
    private String id;
    private String title;
    private String titleEn;
    private String description;
    private String logoUrl;
    private String prefixPath;
    private Integer position;
    /** Only sent to people who may edit the event; the sponsor download link is built from it. */
    private String shareToken;
}
