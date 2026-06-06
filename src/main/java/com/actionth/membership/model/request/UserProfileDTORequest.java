package com.actionth.membership.model.request;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDTORequest {

    private String id;
    private String email;

    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String companyName;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;
    private String phone;
    private String nationality;
    private String idNo;
    private String healthIssues;
    private String bloodType;

    private String emergencyContact;
    private String emergencyRelation;
    private String emergencyPhone;

    private String prefixPath;
    private String pictureUrl;
    private String signatureUrl;
    
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

    private Boolean isApprover;

    private String thumbPictureUrl;
    private String thumbSignaturePath;
    private Boolean tranferApprover;
    private String approverId;

    private Boolean active;

}
