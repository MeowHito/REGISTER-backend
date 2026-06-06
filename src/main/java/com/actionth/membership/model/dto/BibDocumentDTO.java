package com.actionth.membership.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BibDocumentDTO {
    private String participantUuid;
    private String logo;
    private String eventId;
}
