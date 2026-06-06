package com.actionth.membership.model.dto;

import lombok.Data;

@Data
public class SelectionAnswerDto {
    private QuestionDto question;
    private Object value;

    @Data
    public static class QuestionDto {
        private String id;
        private String value;
        private String valueEn;
    }

    @Data
    public static class SelectionValueDto {
        private String id;
        private String value;
        private String valueEn;
        private String inputType;
        private String freeTextValue;
    }
}
