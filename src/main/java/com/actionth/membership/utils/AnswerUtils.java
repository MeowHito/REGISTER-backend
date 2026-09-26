package com.actionth.membership.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.actionth.membership.model.dto.SelectionAnswerDto;
import com.actionth.membership.model.dto.SelectionAnswerDto.SelectionValueDto;

/** Turns a stored questionnaire answer into the text shown in Excel and logs. */
public final class AnswerUtils {

    private AnswerUtils() {
    }

    /** The answer for a question uuid, or "" when the runner did not answer it. */
    public static String answerFor(List<SelectionAnswerDto> answers, String questionId) {
        if (answers == null || questionId == null) {
            return "";
        }
        return answers.stream()
                .filter(a -> a.getQuestion() != null && questionId.equals(a.getQuestion().getId()))
                .map(a -> text(a.getValue()))
                .filter(v -> !v.isEmpty())
                .collect(Collectors.joining(", "));
    }

    public static String text(Object rawValue) {
        if (rawValue instanceof List<?> list) {
            return list.stream()
                    .map(AnswerUtils::single)
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(", "));
        }
        return single(rawValue);
    }

    private static String single(Object rawValue) {
        String value = "";
        String freeText = null;
        boolean isFreeText = false;

        if (rawValue == null) {
            return "";
        }
        if (rawValue instanceof SelectionValueDto dto) {
            value = dto.getValue() != null ? dto.getValue() : "";
            isFreeText = "FREE_TEXT".equals(dto.getInputType());
            freeText = dto.getFreeTextValue();
        } else if (rawValue instanceof Map<?, ?> map) {
            Object valueObj = map.get("value");
            value = valueObj != null ? valueObj.toString() : "";
            isFreeText = "FREE_TEXT".equals(map.get("inputType"));
            Object freeTextObj = map.get("freeTextValue");
            freeText = freeTextObj != null ? freeTextObj.toString() : null;
        } else {
            return rawValue.toString();
        }

        if (isFreeText && freeText != null && !freeText.isEmpty()) {
            return value.isEmpty() ? freeText : value + "; " + freeText;
        }
        return value;
    }
}
