package com.actionth.membership.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.constant.SelectionType;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventSelectionField;
import com.actionth.membership.model.EventSelectionOption;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.dto.QuestionStatsDTO;
import com.actionth.membership.model.dto.SelectionAnswerDto;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.OrderDetailRepository;

import lombok.RequiredArgsConstructor;

/**
 * Counts the answers paid participants gave to an event's registration questions, so organizers
 * can chart them. Answers live as JSON on each {@link OrderDetail} ({@code selectionAnswers});
 * a choice answer carries the option's uuid, so counts follow the option even after a rename.
 */
@Service
@RequiredArgsConstructor
public class QuestionStatsService {

    private static final int MAX_TEXTS = 300;
    private static final int MAX_IMAGES = 60;

    private final EventRepository eventRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final EventAccessService eventAccessService;

    @Transactional(readOnly = true)
    public QuestionStatsDTO getStats(String eventUuid, String eventTypeUuid) {
        Event event = eventRepository.findByUuid(eventUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        eventAccessService.assertCan(event, EventAccessService.Access.READ);

        List<EventType> types = event.getEventTypes().stream()
                .filter(et -> !Boolean.FALSE.equals(et.getActive()))
                .filter(et -> eventTypeUuid == null || eventTypeUuid.equals(et.getUuid()))
                .toList();

        // Participants per distance, paid only (the same people as the participant list / Excel).
        Map<Integer, List<OrderDetail>> byType = new LinkedHashMap<>();
        List<OrderDetail> all = new ArrayList<>();
        for (EventType et : types) {
            List<OrderDetail> paid = orderDetailRepository.getParticipantByEventTypeId(et.getId());
            byType.put(et.getId(), paid);
            all.addAll(paid);
        }

        QuestionStatsDTO dto = QuestionStatsDTO.builder()
                .participants(all.size())
                .pdpaConsentEnabled(Boolean.TRUE.equals(event.getPdpaConsentEnabled()))
                .build();
        for (OrderDetail od : all) {
            Boolean consent = od.getOrder() != null ? od.getOrder().getPdpaConsent() : null;
            if (Boolean.TRUE.equals(consent)) {
                dto.setPdpaAccepted(dto.getPdpaAccepted() + 1);
            } else if (Boolean.FALSE.equals(consent)) {
                dto.setPdpaDeclined(dto.getPdpaDeclined() + 1);
            } else {
                dto.setPdpaUnknown(dto.getPdpaUnknown() + 1);
            }
        }

        for (EventSelectionField field : sorted(event.getSelectionFields())) {
            dto.getQuestions().add(summarise(field, null, all));
        }
        for (EventType et : types) {
            for (EventSelectionField field : sorted(et.getSelectionFields())) {
                dto.getQuestions().add(summarise(field, et.getName(), byType.get(et.getId())));
            }
        }
        return dto;
    }

    private static List<EventSelectionField> sorted(List<EventSelectionField> fields) {
        if (fields == null) {
            return List.of();
        }
        return fields.stream()
                .filter(f -> !Boolean.FALSE.equals(f.getActive()))
                .sorted(Comparator
                        .comparing((EventSelectionField f) -> f.getSection() != null && f.getSection().getPosition() != null
                                ? f.getSection().getPosition() : -1)
                        .thenComparing(f -> f.getPosition() != null ? f.getPosition() : Integer.MAX_VALUE))
                .toList();
    }

    static QuestionStatsDTO.Question summarise(EventSelectionField field, String distanceName,
            List<OrderDetail> participants) {
        SelectionType type = field.getType() != null ? field.getType() : SelectionType.TEXT;
        QuestionStatsDTO.Question q = QuestionStatsDTO.Question.builder()
                .id(field.getUuid())
                .title(field.getTitle())
                .titleEn(field.getTitleEn())
                .type(type.name())
                .sectionTitle(field.getSection() != null ? field.getSection().getTitle() : null)
                .distanceName(distanceName)
                .asked(participants.size())
                .build();

        // Option rows in the organizer's order; answers whose option was since deleted are
        // appended under the label stored with the answer.
        Map<String, QuestionStatsDTO.Option> options = new LinkedHashMap<>();
        if (type == SelectionType.SINGLE || type == SelectionType.MULTIPLE) {
            field.getOptions().stream()
                    .filter(o -> !Boolean.FALSE.equals(o.getActive()))
                    .sorted(Comparator.comparingInt(EventSelectionOption::getPosition))
                    .forEach(o -> options.put(o.getUuid(), QuestionStatsDTO.Option.builder()
                            .id(o.getUuid())
                            .label(o.getValue())
                            .labelEn(o.getValueEn())
                            .inputType(o.getInputType() != null ? o.getInputType() : "FIXED")
                            .build()));
        } else if (type == SelectionType.RATING) {
            for (int score = 1; score <= 5; score++) {
                options.put(String.valueOf(score), QuestionStatsDTO.Option.builder()
                        .id(String.valueOf(score)).label(String.valueOf(score)).inputType("FIXED").build());
            }
        }

        int ratingSum = 0;
        int ratingCount = 0;
        for (OrderDetail od : participants) {
            Object value = answerValue(od.getSelectionAnswers(), field.getUuid());
            if (isBlank(value)) {
                continue;
            }
            boolean answered = false;
            switch (type) {
                case SINGLE, MULTIPLE -> {
                    List<?> picked = value instanceof List<?> list ? list : List.of(value);
                    for (Object choice : picked) {
                        Map<String, Object> c = asMap(choice);
                        String id = str(c.get("id"));
                        String label = str(c.get("value"));
                        if (id == null && label == null) {
                            continue;
                        }
                        String key = id != null ? id : "label:" + label;
                        QuestionStatsDTO.Option row = options.computeIfAbsent(key,
                                k -> QuestionStatsDTO.Option.builder().id(id).label(label)
                                        .labelEn(str(c.get("valueEn")))
                                        .inputType(Objects.toString(c.get("inputType"), "FIXED")).build());
                        row.setCount(row.getCount() + 1);
                        answered = true;
                        String free = str(c.get("freeTextValue"));
                        if ("FREE_TEXT".equals(c.get("inputType")) && free != null) {
                            addCapped(q.getTexts(), free, MAX_TEXTS);
                        }
                    }
                }
                case RATING -> {
                    Integer score = toScore(value);
                    if (score != null) {
                        options.get(String.valueOf(score)).setCount(options.get(String.valueOf(score)).getCount() + 1);
                        ratingSum += score;
                        ratingCount++;
                        answered = true;
                    }
                }
                case IMAGE -> {
                    addCapped(q.getImages(), value.toString(), MAX_IMAGES);
                    answered = true;
                }
                default -> {
                    addCapped(q.getTexts(), value.toString().trim(), MAX_TEXTS);
                    answered = true;
                }
            }
            if (answered) {
                q.setAnswered(q.getAnswered() + 1);
            }
        }
        q.setOptions(new ArrayList<>(options.values()));
        if (ratingCount > 0) {
            q.setAverage(Math.round(ratingSum * 100.0 / ratingCount) / 100.0);
        }
        return q;
    }

    private static Object answerValue(List<SelectionAnswerDto> answers, String questionId) {
        if (answers == null || questionId == null) {
            return null;
        }
        return answers.stream()
                .filter(a -> a.getQuestion() != null && questionId.equals(a.getQuestion().getId()))
                .map(SelectionAnswerDto::getValue)
                .filter(v -> !isBlank(v))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object choice) {
        if (choice instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        if (choice instanceof SelectionAnswerDto.SelectionValueDto v) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId());
            m.put("value", v.getValue());
            m.put("valueEn", v.getValueEn());
            m.put("inputType", v.getInputType());
            m.put("freeTextValue", v.getFreeTextValue());
            return m;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("value", choice == null ? null : choice.toString());
        return m;
    }

    private static Integer toScore(Object value) {
        try {
            int score = (int) Math.round(Double.parseDouble(value.toString().trim()));
            return score >= 1 && score <= 5 ? score : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(Object value) {
        return value == null
                || (value instanceof String s && s.isBlank())
                || (value instanceof List<?> l && l.isEmpty());
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static void addCapped(List<String> list, String value, int max) {
        if (value != null && !value.isBlank() && list.size() < max) {
            list.add(value);
        }
    }
}
