package com.actionth.membership.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventQuestionSection;
import com.actionth.membership.model.EventSelectionField;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.repository.EventQuestionSectionRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.utils.AnswerUtils;

import lombok.RequiredArgsConstructor;

/**
 * Excel of the answers to one sponsor questionnaire section: one row per paid runner, one
 * column per question of the section. Reached from the back office (event access) or through
 * the section's share link (token) that the organizer hands to the sponsor.
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireExportService {

    private final EventQuestionSectionRepository sectionRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ExcelGeneratorService excelGeneratorService;

    @Transactional(readOnly = true)
    public EventQuestionSection findByUuid(String uuid) {
        return sectionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire section not found"));
    }

    @Transactional(readOnly = true)
    public EventQuestionSection findByShareToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResourceNotFoundException("Questionnaire section not found");
        }
        return sectionRepository.findByShareToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire section not found"));
    }

    @Transactional(readOnly = true)
    public byte[] export(EventQuestionSection section) {
        Event event = section.getEvent();
        List<EventSelectionField> questions = event.getSelectionFields().stream()
                .filter(f -> f.getSection() != null && Objects.equals(f.getSection().getId(), section.getId()))
                .sorted(Comparator.comparing(f -> f.getPosition() != null ? f.getPosition() : Integer.MAX_VALUE))
                .toList();

        List<String> columns = new ArrayList<>(List.of("ลำดับ", "BIB", "ชื่อ", "นามสกุล", "ประเภทการแข่งขัน", "ชื่อทีม"));
        for (EventSelectionField q : questions) {
            columns.add(q.getTitle());
        }

        List<List<String>> rows = new ArrayList<>();
        int no = 1;
        for (EventType et : event.getEventTypes()) {
            for (OrderDetail od : orderDetailRepository.getParticipantByEventTypeId(et.getId())) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(no++));
                row.add(Objects.toString(od.getBibNo(), ""));
                row.add(Objects.toString(od.getFirstName(), ""));
                row.add(Objects.toString(od.getLastName(), ""));
                row.add(et.getName());
                row.add(Objects.toString(od.getTeamClub(), ""));
                for (EventSelectionField q : questions) {
                    row.add(AnswerUtils.answerFor(od.getSelectionAnswers(), q.getUuid()));
                }
                rows.add(row);
            }
        }

        Map<String, Object> sheet = new HashMap<>();
        sheet.put("sheetName", "แบบสอบถาม");
        sheet.put("columns", columns.toArray(new String[0]));
        sheet.put("preHeader", List.of(event.getName(), Objects.toString(section.getTitle(), "")));
        sheet.put("datas", rows);
        return excelGeneratorService.generateCustomExcelForMultipleSheets(List.of(sheet));
    }

    /** Safe file name for the download. */
    public String fileName(EventQuestionSection section) {
        String base = Objects.toString(section.getTitle(), "questionnaire").replaceAll("[\\\\/:*?\"<>|]", "_");
        return "Questionnaire_" + base + ".xlsx";
    }
}
