package com.actionth.membership.model.request;

import java.time.OffsetDateTime;
import java.util.List;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

import org.springframework.format.annotation.DateTimeFormat;

import com.actionth.membership.model.dto.OrderDetailShirtDto;
import com.actionth.membership.model.dto.SelectionAnswerDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantDTORequest {

    private String id;    
    private String bibNo;
    private String teamClub;
    private String shirtTypeId;
    private String shirtSizeId;

    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String idNo;
    private String gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;

    private String nationality;

    private String eventTypeId;

    @Email
    private String email;

    private String phone;
    private String phoneCountryCode;

    private String province;

    @Pattern(regexp = "^(A\\+?|A-|B\\+?|B-|AB\\+?|AB-|O\\+?|O-)$", message = "Invalid blood type")
    private String bloodType;

    private String healthIssues;
    private String emergencyContact;
    private String emergencyRelation;

    private String emergencyPhone;
    private String emergencyPhoneCountryCode;

    /** Extra (finisher / special) shirts; the race shirt is shirtTypeId/shirtSizeId. Null = leave as is. */
    private List<OrderDetailShirtDto> shirts;

    private String address;
    private String amphoe;
    private String district;
    private String zipcode;

    private String shippingAddress;
    private String shippingProvince;
    private String shippingAmphoe;
    private String shippingDistrict;
    private String shippingZipcode;

    private List<SelectionAnswerDto> selectionAnswers;

    /** Move to a distance whose quota is already full anyway (the user was warned first). */
    private Boolean confirmOverQuota;
}
