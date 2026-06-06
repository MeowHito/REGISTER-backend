package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRegistrationDTO {

    private String eventId;
    private String eventName;
    private int participantByEvent;

    @Builder.Default
    private Map<String, Map<String, Integer>> participantByEventType = new HashMap<>();

    private int capacityByEvent;
    private Integer paidByEvent;

    @Builder.Default
    private Map<String, Integer> paidByEventType = new HashMap<>();

    private Integer paidPayment;
    private Integer unpaidPayment;
    private Integer pendingPayment;

    @Builder.Default
    private Map<String, Integer> genderByEvent = new HashMap<>();

    @Builder.Default
    private Map<String, Map<String, Integer>> genderByEventType = new HashMap<>();

    @Builder.Default
    private Map<String, Map<String, Integer>> ageGroupByEvent = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Map<String, Map<String, Integer>>> ageGroupByEventType = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Map<String, Integer>> shirtByEvent = new HashMap<>();

    @Builder.Default
    private Map<String, Map<String, Map<String, Integer>>> shirtByEventType = new HashMap<>();

    @Builder.Default
    private Map<String, Integer> participantByProvince = new HashMap<>();

    @Builder.Default
    private List<TimeBucketCountDto> participantRegisterDate = new ArrayList<>();

    private OffsetDateTime registrationOpen;
    private OffsetDateTime registrationClose;
    private Integer countInternalParticipant;
    private Integer countExternalParticipant;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeBucketCountDto {
        private OffsetDateTime dateTime;
        private Integer count;
    }

}
