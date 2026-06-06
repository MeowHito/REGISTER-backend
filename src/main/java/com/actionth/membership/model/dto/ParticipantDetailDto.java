package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class ParticipantDetailDto {

    private String participantId;
    private String bibNo;

    // Order
    private String orderNo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime registerDate;

    // Personal
    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;
    private Integer age;
    private String nationality;
    private String idNo;
    private String pictureUrl;
    private String prefixPath;

    // Contact
    private String email;
    private String phone;
    private String address;
    private String province;
    private String amphoe;
    private String district;
    private String zipcode;

    // Health & Emergency
    private String bloodType;
    private String healthIssues;
    private String emergencyContact;
    private String emergencyRelation;
    private String emergencyPhone;

    // Event
    private String eventTypeName;
    private String ageGroupName;
    private String teamClub;

    // Shirt & Delivery
    private Boolean receiveShirt;
    private String shirtTypeName;
    private String shirtSizeName;
    private String deliveryMethod;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingAmphoe;
    private String shippingDistrict;
    private String shippingZipcode;

    // Dynamic Q&A
    private List<SelectionAnswerDto> selectionAnswers;

    // Whether sensitive fields are masked
    private Boolean masked;
}
