package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sliders", indexes = {
    @Index(name = "idx_position", columnList = "position"),
    @Index(name = "idx_active", columnList = "active")
})
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class Slider extends StandardFields {

    @Column(columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(columnDefinition = "TEXT")
    private String descriptionTh;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 50)
    private String alignment;

    @Column(nullable = false)
    private Integer position;
}
