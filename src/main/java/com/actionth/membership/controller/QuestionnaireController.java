package com.actionth.membership.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import com.actionth.membership.model.EventQuestionSection;
import com.actionth.membership.service.EventAccessService;
import com.actionth.membership.service.EventAccessService.Access;
import com.actionth.membership.service.ExcelGeneratorService;
import com.actionth.membership.service.QuestionnaireExportService;

import lombok.RequiredArgsConstructor;

/**
 * Sponsor questionnaire answers as Excel: the back office downloads by section id (event read
 * access), a sponsor downloads through the share token in the link the organizer sent.
 */
@RestController
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireExportService questionnaireExportService;
    private final EventAccessService eventAccessService;

    @GetMapping("/api/questionnaire/export")
    public ResponseEntity<byte[]> exportForBackOffice(@RequestParam("sectionId") String sectionId) {
        EventQuestionSection section = questionnaireExportService.findByUuid(sectionId);
        eventAccessService.assertCan(section.getEvent(), Access.READ);
        return excel(section);
    }

    @GetMapping("/public-api/questionnaire/export")
    public ResponseEntity<byte[]> exportForSponsor(@RequestParam("token") String token) {
        EventQuestionSection section = questionnaireExportService.findByShareToken(token);
        return excel(section);
    }

    private ResponseEntity<byte[]> excel(EventQuestionSection section) {
        byte[] body = questionnaireExportService.export(section);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setAccessControlExposeHeaders(ExcelGeneratorService.ACCESS_HEADERS);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(UriUtils.encode(questionnaireExportService.fileName(section), StandardCharsets.UTF_8))
                .build());
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
