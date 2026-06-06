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

    private String orderNo;

    private String email;
    private String phone;
    private String province;
    private String bloodType;
    private String healthIssues;
    private String emergencyContact;
    private String emergencyRelation;
    private String emergencyPhone;

    private List<SelectionAnswerDto> selectionAnswers;
    private List<EventSelectionFieldDto> selectionFields;
}
