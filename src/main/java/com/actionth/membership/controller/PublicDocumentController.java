package com.actionth.membership.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.model.dto.BibDocumentDTO;
import com.actionth.membership.service.AppConfigService;
import com.actionth.membership.service.ReportService;
import com.actionth.membership.service.impl.UserServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/public-api/doc")
public class PublicDocumentController {

    @Value("${app.report-path}")
    private String reportPath;

    @Autowired
    ReportService reportService;

    @Autowired
    AppConfigService appConfigService;

    @Autowired
    UserServiceImpl userService;

    private static final List<String> ACCESS_HEADERS = Collections
            .unmodifiableList(Arrays.asList("Content-Type", "Content-Disposition"));

    public Timestamp getCurrentTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"));
        return new Timestamp(calendar.getTimeInMillis());
    }

    public String optString(String value) {
        return Optional.ofNullable(value).orElse("");
    }

    @PostMapping("/getBibDocument")
    public ResponseEntity<byte[]> getContractDocument(
            @RequestHeader Map<String, String> headers,
            @RequestBody BibDocumentDTO runner)
            throws IllegalStateException, Exception {

        HttpHeaders resHeader = new HttpHeaders();
        resHeader.setContentType(MediaType.APPLICATION_PDF);
        resHeader.setAccessControlExposeHeaders(ACCESS_HEADERS);
        byte[] data = null;
        String template = "/bib_report.jrxml";
        String logoShirt = reportPath + "/logoShirt.png";
        String tmp = System.getProperty("java.io.tmpdir") + "/temp-" + getCurrentTime().getTime() + ".png";
        try (InputStream in = getClass().getResourceAsStream(logoShirt)) {
            Files.copy(in, Paths.get(tmp), StandardCopyOption.REPLACE_EXISTING);
        }
        File imageShirt = new File(tmp);

        String logoBib = reportPath + "/logoBib.png";
        String tmpBib = System.getProperty("java.io.tmpdir") + "/temp-" + getCurrentTime().getTime() + ".png";
        try (InputStream in = getClass().getResourceAsStream(logoBib)) {
            Files.copy(in, Paths.get(tmpBib), StandardCopyOption.REPLACE_EXISTING);
        }
        File imageBib = new File(tmpBib);
        try {
            Map<String, Object> paramMap = new HashMap<>();

            paramMap.put("logoShirt", imageShirt.getAbsolutePath());
            paramMap.put("logoBib", imageBib.getAbsolutePath());
            paramMap.put("participantUuid", optString(runner.getParticipantUuid()));

            data = reportService.generateReportImageQuery(template, paramMap);

            ContentDisposition contentDisposition = ContentDisposition.builder("attachment").filename("BibDocument-" + getCurrentTime().getTime() + ".jpg").build();
            resHeader.setContentDisposition(contentDisposition);
        } catch (EmptyResultDataAccessException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } finally {
            try {
                Files.delete(imageShirt.toPath());
                Files.delete(imageBib.toPath());
            } catch (IOException e) {
                log.error("Failed to delete temp files", e);
            }
        }
        return new ResponseEntity<>(data, resHeader, HttpStatus.OK);
    }
}
