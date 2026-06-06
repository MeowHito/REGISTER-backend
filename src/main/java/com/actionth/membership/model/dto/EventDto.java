package com.actionth.membership.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private String id;
    private String name;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    private String organizerName;
    private String organizerId;
    private String location;
    private String description;
    private String logoUrl;
    private String pictureUrl;
    private String prefixPath;
    private CountryStateDto province;
    private String provinceId;
    private String type;
    private String link;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startRegistrationDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endRegistrationDate;
    private BigDecimal shippingFee;

    private String generalInfoTitle;
    private String eventTypeTitle;

    private String eventPrimaryColor;
    private String eventSecondaryColor;
    private String eventFontColor;

    private Boolean isDraft;
    private Boolean showChecklist;

    private List<EventConditionDto> eventConditions;
    private List<EventDetailDto> eventDetails;
    private List<PaymentTypeDto> paymentTypes;
    private List<EventTypeDto> eventTypes;
    private List<ShirtTypeDto> shirtTypes;
    private List<EventSelectionFieldDto> selectionFields;
}
