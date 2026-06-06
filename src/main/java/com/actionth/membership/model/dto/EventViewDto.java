package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventViewDto {
    private String id;
    private String name;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    private String location;
    private CountryStateDto province;
    private String type;
    private String link;
    private String logoUrl;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startRegistrationDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endRegistrationDate;
    private Boolean isDraft;
    private String eventStatus;
    private String organizerId;
    private EventPermissionSummaryDto permission;
}
