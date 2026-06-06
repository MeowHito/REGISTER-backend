package com.actionth.membership.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@EqualsAndHashCode(callSuper = true)
public class OrderDetail extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("order-orderDetail")
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "eventTypeId")
    private EventType eventType;

    @ManyToOne
    @JoinColumn(name = "shirtTypeId")
    private ShirtType shirtType;

    @ManyToOne
    @JoinColumn(name = "shirtSizeId")
    private ShirtSize shirtSize;

    @ManyToOne
    @JoinColumn(name = "pricingId")
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
    private String nationality;
    private String idNo;
    private String healthIssues;
    private String bloodType;

    private String emergencyContact;
    private String emergencyRelation;
    private String emergencyPhone;

    private String teamClub;

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
}
