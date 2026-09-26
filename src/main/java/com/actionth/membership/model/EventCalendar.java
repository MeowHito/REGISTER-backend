package com.actionth.membership.model;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "eventCalendar", indexes = {
        @Index(name = "IDX_eventCalendar_source", columnList = "source, sourceId") })
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class EventCalendar extends StandardFields {
    
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

    /** Set when the row was pulled from another site (e.g. "joggingandrunning.com"); null for manual submissions. */
    private String source;
    /** The event's id on the source site, used to upsert on re-sync. */
    private String sourceId;
    /** The event's page on the source site (credit / "see original"). */
    private String sourceUrl;
    /** Last modified time on the source site; the sync watermark is derived from it. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime sourceUpdatedAt;

}
