package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;

@Data
public class ParticipantViewDto {

    private String bibNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime registerDate;

    private String teamClub;

    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;

    private String nationality;
    private String orderNo;
    private String eventType;
    private String deliveryMethod;
    private String shirtSize;
    private String participantId;
    private String ageGroupName;
}
