package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "ageGroup")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class AgeGroup extends StandardFields {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventTypeId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("eventType-ageGroups")
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer minAge;
    private Integer maxAge;

    private Integer position;

    public enum Gender {
        male, female
    }
}