package com.actionth.membership.model.request;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventCalendarDTO {
    
    private String eventId;
    private String eventName;
    private String eventType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    
    private String location;
    private String extraDetail;
    private String link;
    private String submitterName;
    private String email;
    private String phone;
    private Boolean isApproved;
    private String rejectReason;

}
