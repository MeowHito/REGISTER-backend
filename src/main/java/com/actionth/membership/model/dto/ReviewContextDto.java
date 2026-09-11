package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewContextDto {

    private String orderNo;
    private OrderState currentState;
    private List<OfferedCombo> offeredCombos;
    private List<ReceivedPayment> receivedPayments;
    private List<TimelineEvent> timeline;
    private Suggestion suggestion;
    private List<FieldChange> expectedChanges;
    private boolean multipleSuccessfulPayments;
    private boolean noMatch;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderState {
        private String paymentStatus;
        private String reviewReason;
        private String paymentMethod;
        private Double feePercent;
        private Double fee;
        private Double totalAmountWithFee;
        private String scbTransactionId;
        private OffsetDateTime paymentDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferedCombo {
        private String paymentMethod;
        private Double feePercent;
        private Double fee;
        private Double totalAmountWithFee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivedPayment {
        private String provider;
        private String transactionId;
        private String resolvedMethod;
        private Double amount;
        private String payerName;
        private String reference;
        private OffsetDateTime receivedDateTime;
        private String logType;
        private String reasonType;
        private boolean successful;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private OffsetDateTime time;
        private String type;
        private String provider;
        private String method;
        private Double feePercent;
        private Double amount;
        private String reasonType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldChange {
        private String field;
        private String current;
        private String after;
        private boolean changed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String transactionId;
        private OfferedCombo combo;
        private boolean matched;
    }
}
