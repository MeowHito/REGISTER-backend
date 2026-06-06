package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "pricing")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class Pricing extends StandardFields {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventTypeId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("eventType-pricing")
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paymentTypeId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("paymentType-pricing")
    private PaymentType paymentType;

    private BigDecimal price;
    private Integer quota;
}
