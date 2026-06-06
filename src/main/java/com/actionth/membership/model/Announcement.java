package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import javax.persistence.*;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "announcement")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class Announcement extends StandardFields {

    @JsonBackReference("announcement-event")
    @ManyToOne
    @JoinColumn(name = "eventId")
    private Event event;

    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String detail;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startDate;

    private String prefixPath;
    private Boolean isRead;
}
