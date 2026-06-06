package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {

        private int participantByEvent;
        
        @Builder.Default
        private Map<String, Map<String, Integer>> participantByEventType = new HashMap<>();
        
        private int capacityByEvent;

        private int progressOperation;
        private String operationStatusCode;
        private long elapsedDays;
        private int progressApplicants;
        private Integer paidByEvent;
        private int progressPayment;
        private Long totalRegistrationFee;
        private Long totalShippingFee;
        private Long totalNetRevenue;

        @Builder.Default
        private List<Map<String, Object>> paymentStatusByMethod = new ArrayList<>();

        @Builder.Default
        private List<Map<String, Object>> participantsPerDay = new ArrayList<>();

        @Builder.Default
        private List<Map<String, Object>> paidParticipantsPerDay = new ArrayList<>();

        @Builder.Default
        private List<Map<String, Object>> failureReasons = new ArrayList<>();

        @Builder.Default
        private Map<String, Integer> paidByEventType = new HashMap<>();

}