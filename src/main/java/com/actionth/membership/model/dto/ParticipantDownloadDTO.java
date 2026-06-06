package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;

@Data
public class ParticipantDownloadDTO {

    private String id;
    private String bibNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime registerDate;

    private String teamClub;
    private ShirtSizeDto shirtSize;
    private Boolean rules;
    private String deliveryMethod;

    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String idNo;
    private String gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;

    private String email;
    private String phone;
    private String nationality;
    private String bloodType;
    private String healthIssues;
    private String emergencyContact;
    private String emergencyPhone;

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

    private String orderNo;
    
    private String selectedAnswer;
}
