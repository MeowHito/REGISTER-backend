package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "event")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Event extends StandardFields {
    private String name;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    private String organizerName;
    private String location;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provinceId", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private CountryState province;

    private String type;
    @Column(unique = true)
    private String link;

    private String pictureUrl;
    private String prefixPath;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startRegistrationDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endRegistrationDate;
    private BigDecimal shippingFee;

    private String generalInfoTitle;
    private String eventTypeTitle;

    private String eventPrimaryColor;
    private String eventSecondaryColor;
    private String eventFontColor;

    private Boolean isDraft;
    private Boolean showChecklist;

    /**
     * Admin-only switch for rehearsing the registration flow: orders on a
     * test-mode event skip the payment gateway and go straight to SUCCESS.
     */
    private Boolean testMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizerId")
    @JsonBackReference("user-event")
    @ToString.Exclude
    private User organizer;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-conditions")
    @Builder.Default
    @ToString.Exclude
    private List<EventCondition> eventConditions = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-details")
    @Builder.Default
    @ToString.Exclude
    private List<EventDetail> eventDetails = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-paymentTypes")
    @Builder.Default
    @ToString.Exclude
    private List<PaymentType> paymentTypes = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-eventTypes")
    @Builder.Default
    @ToString.Exclude
    private List<EventType> eventTypes = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-shirtTypes")
    @Builder.Default
    @ToString.Exclude
    private List<ShirtType> shirtTypes = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-selectionFields")
    @Builder.Default
    @ToString.Exclude
    private List<EventSelectionField> selectionFields = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-addOns")
    @Builder.Default
    @ToString.Exclude
    private List<EventAddOn> addOns = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("event-eventPermissions")
    @Builder.Default
    @ToString.Exclude
    private List<EventPermission> eventPermissions = new ArrayList<>();
}