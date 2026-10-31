package com.actionth.membership.model.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Answers to an event's registration questions, summarised for the back-office stats page. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatsDTO {

    /** Paid participants in scope (whole event, or the picked distance). */
    private int participants;

    private boolean pdpaConsentEnabled;
    /** Participants whose order accepted / declined the PDPA prompt; unknown = order made before it was on. */
    private int pdpaAccepted;
    private int pdpaDeclined;
    private int pdpaUnknown;

    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private String id;
        private String title;
        private String titleEn;
        /** SINGLE | MULTIPLE | TEXT | RATING | IMAGE */
        private String type;
        private String sectionTitle;
        /** Distance name for a per-distance question; null when the whole event is asked. */
        private String distanceName;
        /** Participants who were shown the question. */
        private int asked;
        /** Participants who answered it. */
        private int answered;
        /** SINGLE / MULTIPLE: one row per option; RATING: one row per score 1-5. */
        @Builder.Default
        private List<Option> options = new ArrayList<>();
        /** RATING only. */
        private Double average;
        /** Typed answers: TEXT answers and the text of "ระบุ" options (newest participants last, capped). */
        @Builder.Default
        private List<String> texts = new ArrayList<>();
        /** IMAGE only: uploaded picture URLs (capped). */
        @Builder.Default
        private List<String> images = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String id;
        private String label;
        private String labelEn;
        /** FIXED, or FREE_TEXT for an "other, please specify" choice. */
        private String inputType;
        private int count;
    }
}
