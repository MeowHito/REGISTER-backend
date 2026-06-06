package com.actionth.membership.model.request;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantUploadDTORequest {

    private List<ParticipantUploadDTO> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParticipantUploadDTO {
        private String id;
        private String firstName;
        private String lastName;
        private String firstNameEn;
        private String lastNameEn;
        private String idNo;
        private String gender;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private OffsetDateTime birthDate;

        private String nationality;
        private String email;
        private String phone;
        private String address;
        private String province;
        private String amphoe;
        private String district;
        private String zipcode;
        private String bloodType;
        private String healthIssues;
        private String emergencyContact;
        private String emergencyPhone;

        private String shirtSizeId;
        private String teamClub;
        private String bibNo;

        private String shippingAddress;
        private String shippingProvince;
        private String shippingAmphoe;
        private String shippingDistrict;
        private String shippingZipcode;
    }
}
