package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;

@Data
public class ParticipantDTO {

    private String id;
    private String bibNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime registerDate;

    private String teamClub;

    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String idNo;
    private String gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;

    private String nationality;

    private String shirtSizeId;
    private String shirtTypeId;
    private String eventTypeId;
    private String eventTypeName;
    private String shirtSizeName;
    private String shirtTypeName;

    private String orderNo;

    private String email;
    private String phone;
    private String phoneCountryCode;
    private String province;
    private String bloodType;
    private String healthIssues;
    private String emergencyContact;
    private String emergencyRelation;
    private String emergencyPhone;
    private String emergencyPhoneCountryCode;
    private Integer teamGroup;
    /** Every garment: the race shirt first, then finisher / special ones. */
    private List<OrderDetailShirtDto> shirts;

    private String address;
    private String amphoe;
    private String district;
    private String zipcode;

    private String deliveryMethod;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingAmphoe;
    private String shippingDistrict;
    private String shippingZipcode;

    /** Non-null once an admin/organizer edited this runner by hand; the list highlights the row. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime manualEditedTime;
    private String manualEditedBy;
    /** Full edit history, only on the single-participant endpoint. */
    private List<ParticipantEditLogDto> manualEdits;

    private List<SelectionAnswerDto> selectionAnswers;
    private List<EventSelectionFieldDto> selectionFields;

    /** Add-ons on this row: the runner's own, plus per-order ones on the order's first applicant. */
    private List<OrderAddOnDto> addOns;
}
