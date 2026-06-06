package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventCreationDTORequest {

    private String uuid;
    private String organizerName;
    private String name;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    private String location;
    private String description;
    private JsonNode extraDetail;
    private String email;
    private String website;
    private String facebook;
    private String contactName;
    private String contactTel;
    private List<EventTypeDTORequest> eventTypes = new ArrayList<>();
    private List<PayTypeDTORequest> payTypes = new ArrayList<>();
    private List<PaymentRateDTORequest> paymentRates = new ArrayList<>();

}
