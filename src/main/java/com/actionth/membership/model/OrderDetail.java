package com.actionth.membership.model;

import lombok.AllArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.ArrayList;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.actionth.membership.converter.SelectionAnswerConverter;
import com.actionth.membership.model.dto.SelectionAnswerDto;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "orderDetail")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class OrderDetail extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("order-orderDetail")
    @ToString.Exclude
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "eventTypeId")
    @ToString.Exclude
    private EventType eventType;

    @ManyToOne
    @JoinColumn(name = "shirtTypeId")
    @ToString.Exclude
    private ShirtType shirtType;

    @ManyToOne
    @JoinColumn(name = "shirtSizeId")
    @ToString.Exclude
    private ShirtSize shirtSize;

    @ManyToOne
    @JoinColumn(name = "pricingId")
    @ToString.Exclude
    private Pricing pricing;

    private Boolean isSelf;
    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;
    private Integer age;
    private String email;
    private String phone;
    /** Dialling code of {@link #phone}, e.g. "+66"; null means Thailand for rows from before it existed. */
    @Column(length = 8)
    private String phoneCountryCode;
    private String nationality;
    private String idNo;
    private String healthIssues;
    private String bloodType;

    private String emergencyContact;
    private String emergencyRelation;
    private String emergencyPhone;
    @Column(length = 8)
    private String emergencyPhoneCountryCode;

    private String teamClub;

    /**
     * Groups the members of one team inside an order (1, 2, ...) for team distances; null for
     * individual registrations.
     */
    private Integer teamGroup;

    /** Finisher / special shirts picked besides the race shirt. */
    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("orderDetail-shirts")
    @Builder.Default
    @ToString.Exclude
    private List<OrderDetailShirt> shirts = new ArrayList<>();

    private String deliveryMethod;

    private Boolean couponUsed;
    private Double price;
    private Double discountShirt;
    private Double couponDiscount;
    private Double shippingFee;
    private Double netPrice;

    private String pictureUrl;
    private String prefixPath;

    private String address;
    private String province;
    private String amphoe;
    private String district;
    private String zipcode;

    private String shippingAddress;
    private String shippingProvince;
    private String shippingAmphoe;
    private String shippingDistrict;
    private String shippingZipcode;

    private String bibNo;
    private Boolean rules;
    private Boolean receiveShirt;

    @Convert(converter = SelectionAnswerConverter.class)
    @Column(columnDefinition = "json")
    private List<SelectionAnswerDto> selectionAnswers;

    /**
     * Set when an admin or organizer changes this runner by hand in the back office
     * (not by the runner, not by the Excel upload); the participant list highlights such rows.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime manualEditedTime;
    private String manualEditedBy;

    /** JSON list of {@code ParticipantEditLogDto}: every manual edit, oldest first. */
    @Column(columnDefinition = "longtext")
    private String manualEditLog;
}
