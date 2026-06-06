package com.actionth.membership.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.actionth.membership.model.dto.ContractDocumentDTO;
import com.actionth.membership.model.request.ImageDTORequest;
import com.actionth.membership.model.request.SummaryFinanceRequestDTO;
import com.actionth.membership.model.request.ZipDTORequest;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.AppConfigService;
import com.actionth.membership.service.ReportService;
import com.actionth.membership.service.impl.UserServiceImpl;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/doc")
public class DocumentController {

    @Value("${app.report-path}")
    private String reportPath;

    @Autowired
    ReportService reportService;

    @Autowired
    AppConfigService appConfigService;

    @Autowired
    private AWSService awsService;

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

    @PostMapping("/getContractDocument")
    public ResponseEntity<byte[]> getContractDocument(
            @RequestHeader Map<String, String> headers,
            @RequestBody ContractDocumentDTO contract)
            throws IllegalStateException, Exception {

        HttpHeaders resHeader = new HttpHeaders();
        resHeader.setContentType(MediaType.APPLICATION_PDF);
        resHeader.setAccessControlExposeHeaders(ACCESS_HEADERS);
        byte[] data = null;
        String template = "/Contract.jrxml";
        String logo = reportPath + "/logo.png";
        String tmp = System.getProperty("java.io.tmpdir") + "/temp-" + getCurrentTime().getTime() + ".png";
        try (InputStream in = getClass().getResourceAsStream(logo)) {
            Files.copy(in, Paths.get(tmp), StandardCopyOption.REPLACE_EXISTING);
        }
        File image = new File(tmp);
        File sealImage = null;
        try {
            Map<String, Object> paramMap = new HashMap<>();

            String seal = appConfigService.findFirstByName("providerSealPath");
            if (seal == null || seal.isEmpty()) {
                paramMap.put("providerSeal", optString(seal));
            } else {
                String sealPath = reportPath + seal;
                String sealTemp = System.getProperty("java.io.tmpdir") + "/temp-" + getCurrentTime().getTime() + ".png";
                try (InputStream in = getClass().getResourceAsStream(sealPath)) {
                    Files.copy(in, Paths.get(sealTemp), StandardCopyOption.REPLACE_EXISTING);
                }
                sealImage = new File(sealTemp);

                paramMap.put("providerSeal", optString(sealImage.getAbsolutePath()));
            }

            String providerSignature = userService.getApproverSignatureImg();
            if (providerSignature == null || providerSignature.isEmpty()) {
                paramMap.put("providerSignature", optString(providerSignature));
            } else {
                String prefixProfile = "userData";
                String publicUserProfileUrl = awsService.getPublicUrl(prefixProfile, providerSignature);
                paramMap.put("providerSignature", optString(publicUserProfileUrl));
            }
            String customerSignature = contract.getCustomerSignature();
            if (customerSignature == null || customerSignature.isEmpty()) {
                paramMap.put("customerSignature", optString(customerSignature));
            } else {
                String prefixContract = "contract";
                String publicCustomerSignatureUrl = awsService.getPublicUrl(prefixContract, customerSignature);
                paramMap.put("customerSignature", optString(publicCustomerSignatureUrl));
            }

            paramMap.put("logo", image.getAbsolutePath());
            paramMap.put("runNo", optString(contract.getRunNo()));
            paramMap.put("contractDate", optString(contract.getContractDate()));
            paramMap.put("detail", optString(contract.getDetail()));
            paramMap.put("customerCompany", optString(contract.getCustomerCompany()));
            paramMap.put("organizer", optString(contract.getOrganizer()));
            paramMap.put("tel", optString(contract.getTel()));
            paramMap.put("address", optString(contract.getAddress()));
            paramMap.put("taxNo", optString(contract.getTaxNo()));
            paramMap.put("event", optString(contract.getEvent()));
            paramMap.put("providerName", optString(contract.getProviderName()));
            paramMap.put("providerPosition", optString(contract.getProviderPosition()));
            paramMap.put("customerSeal", optString(contract.getCustomerSeal()));
            paramMap.put("customerName", contract.getCustomerName());
            paramMap.put("customerPosition", contract.getCustomerPosition());

            data = reportService.generateReport(template, paramMap);

            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename("ContractDocument-" + getCurrentTime().getTime() + ".pdf").build();
            resHeader.setContentDisposition(contentDisposition);

        } catch (EmptyResultDataAccessException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } finally {
            try {
                Files.deleteIfExists(image.toPath());
                if (sealImage != null) {
                    Files.deleteIfExists(sealImage.toPath());
                }
            } catch (IOException e) {
                log.error("Failed to delete temp files", e);
            }
        }
        return new ResponseEntity<>(data, resHeader, HttpStatus.OK);
    }

    @PostMapping("/zipAnnouncement")
    public ResponseEntity<byte[]> zipAnnouncement(@RequestBody ZipDTORequest request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);

        for (ZipDTORequest.ImageDTO img : request.getImages()) {
            try (InputStream in = new URL(img.getUrl()).openStream()) {
                zipOut.putNextEntry(new ZipEntry("images_" + request.getName() + "/" + img.getFilename()));
                in.transferTo(zipOut);
                zipOut.closeEntry();
            } catch (Exception e) {
                // skip image if failed to load
            }
        }

        zipOut.close();

        byte[] zipBytes = baos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setAccessControlExposeHeaders(ACCESS_HEADERS);
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename("announcement_" + request.getName() + "_" + getCurrentTime().getTime() + ".zip").build();
        headers.setContentDisposition(contentDisposition);

        return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/getSummaryFinanceDocument")
    public ResponseEntity<byte[]> getSummaryFinanceDocument(
            @RequestBody SummaryFinanceRequestDTO req)
            throws IllegalStateException, Exception {

        HttpHeaders resHeader = new HttpHeaders();
        resHeader.setContentType(MediaType.APPLICATION_PDF);
        resHeader.setAccessControlExposeHeaders(ACCESS_HEADERS);
        byte[] data = null;
        String template = "/summary_finance_report.jrxml";
        File sealImage = null;
        try {
            Map<String, Object> paramMap = new HashMap<>();

            String seal = appConfigService.findFirstByName("providerSealPath");
            if (seal == null || seal.isEmpty()) {
                paramMap.put("providerSeal", optString(seal));
            } else {
                String sealPath = reportPath + seal;
                String sealTemp = System.getProperty("java.io.tmpdir") + "/temp-" + getCurrentTime().getTime() + ".png";
                try (InputStream in = getClass().getResourceAsStream(sealPath)) {
                    Files.copy(in, Paths.get(sealTemp), StandardCopyOption.REPLACE_EXISTING);
                }
                sealImage = new File(sealTemp);

                paramMap.put("sealUrl", optString(sealImage.getAbsolutePath()));
            }

            String providerSignature = userService.getApproverSignatureImg();
            if (providerSignature == null || providerSignature.isEmpty()) {
                paramMap.put("signatureUrl", optString(providerSignature));
            } else {
                String prefixProfile = "userData";
                String publicUserProfileUrl = awsService.getPublicUrl(prefixProfile, providerSignature);
                paramMap.put("signatureUrl", optString(publicUserProfileUrl));
            }

            paramMap.put("id", optString(req.getId()));
            paramMap.put("startDate", optString(req.getStartDate()));
            paramMap.put("endDate", optString(req.getEndDate()));
            paramMap.put("remark", optString(req.getRemark()));

            data = reportService.generateReportQuery(template, paramMap);

            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename("SummaryFinanceDocument-" + getCurrentTime().getTime() + ".pdf").build();
            resHeader.setContentDisposition(contentDisposition);

        } catch (EmptyResultDataAccessException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } finally {
            if (sealImage != null) {
                try {
                    Files.deleteIfExists(sealImage.toPath());
                } catch (IOException e) {
                    log.error("Failed to delete temp files", e);
                }
            }
        }
        return new ResponseEntity<>(data, resHeader, HttpStatus.OK);
    }

    @PostMapping("/downloadImage")
    public ResponseEntity<byte[]> downloadImage(@RequestBody ImageDTORequest img) throws IOException {
        try (InputStream in = new URL(img.getUrl()).openStream()) {
            byte[] imageBytes = in.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            String contentType = Optional.ofNullable(img.getFileType())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setAccessControlExposeHeaders(List.of("Content-Disposition"));
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(img.getFilename())
                    .build();
            headers.setContentDisposition(contentDisposition);

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
