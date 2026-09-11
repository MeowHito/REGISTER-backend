package com.actionth.membership.model;

import com.actionth.membership.constant.SelectionType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "eventSelectionField")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EventSelectionField extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventId")
    @JsonBackReference("event-selectionFields")
    @ToString.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventTypeId")
    @JsonBackReference("eventType-selectionFields")
    @ToString.Exclude
    private EventType eventType;

    private String title;

    private String titleEn;

    @Enumerated(EnumType.STRING)
    private SelectionType type;

    private boolean required;

    @OneToMany(mappedBy = "selectionField", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<EventSelectionOption> options = new ArrayList<>();

}
