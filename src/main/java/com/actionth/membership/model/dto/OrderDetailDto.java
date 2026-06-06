package com.actionth.membership.model.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;


@Data
public class OrderDetailDto {
    private String id;
    private String eventTypeId;
    private String shirtTypeId;
    private String shirtSizeId;
    private String pricingId;

    private Boolean isSelf;
    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;
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

    private List<SelectionAnswerDto> selectionAnswers;
}