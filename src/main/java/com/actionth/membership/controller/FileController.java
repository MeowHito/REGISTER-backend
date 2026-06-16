package com.actionth.membership.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.actionth.membership.response.Response;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.CouponService;
import com.actionth.membership.service.EventService;
import com.actionth.membership.service.ExcelGeneratorService;
import com.actionth.membership.service.ParticipantService;
import com.actionth.membership.service.SummaryReportService;

@RestController
@RequestMapping("/api/file")
public class FileController {

	@Autowired
	private ParticipantService participantService;

	@Autowired
	private EventService eventService;

	@Autowired
	private ExcelGeneratorService excelGeneratorService;

	@Autowired
	private AWSService awsService;

	@Autowired
	private SummaryReportService summaryReportService;

	@Autowired
	private CouponService couponService;

	@PostMapping(path = "/uploadFile")
	public ResponseEntity<Response<String>> uploadFile(
			@RequestParam("prefix") String prefix,
			@RequestParam("file") MultipartFile multipartFile,
			@RequestParam(value = "isPublic", required = false) Boolean isPublic) throws IllegalStateException, IOException {

		// ตรวจสอบชื่อไฟล์ต้นฉบับ
		String fileName = Optional.ofNullable(multipartFile.getOriginalFilename()).orElse("");
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());

		// แยกชื่อไฟล์และส่วนขยาย
		int index = fileName.lastIndexOf("/");
		String extractedFileName = index > 0 ? fileName.substring(index + 1) : fileName;

		index = extractedFileName.lastIndexOf(".");
		if (index > 0) {
			String extension = extractedFileName.substring(index + 1);
			String name = extractedFileName.substring(0, index);
			fileName = name + "-" + timestamp.getTime() + "." + extension;
		} else {
			fileName += "-" + timestamp.getTime();
		}

		// สร้างไฟล์ชั่วคราวใน temp directory
		File file = new File(System.getProperty("java.io.tmpdir"), fileName);
		multipartFile.transferTo(file);

		// อัปโหลดไฟล์ไปยัง AWS — ลบไฟล์ชั่วคราวเสมอแม้ upload จะ throw
		String uploadedFileName;
		try {
			uploadedFileName = awsService.uploadFile(prefix, file, isPublic);
		} finally {
			Files.deleteIfExists(file.toPath());
		}

		// สร้าง Response
		Response<String> response = new Response<>();
		response.setData(uploadedFileName);
		response.setMessage("success");
		response.setSuccess(true);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/downloadParticipant")
	public ResponseEntity<byte[]> downloadParticipant(@RequestParam("id") String id) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccessControlExposeHeaders(ExcelGeneratorService.ACCESS_HEADERS);
		byte[] body = null;

		try {
			Map<String, Object> title = eventService.findIdAndNameByUuid(id);

			List<Map<String, Object>> dataList = participantService
					.getAllParticipantDownload((Integer) title.get("id"));
			body = excelGeneratorService.generateCustomExcelForMultipleSheets(dataList);

			String encodedTitle = UriUtils.encode((String) title.get("name"), StandardCharsets.UTF_8);

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename("Participant_" + encodedTitle + ".xlsx").build();
			headers.setContentDisposition(contentDisposition);

		} catch (Exception ex) {
			headers.setContentType(MediaType.APPLICATION_JSON);
			Response<Object> response = Response.builder().success(false).message("An error occurred").data(ex.getMessage())
					.build();
			return new ResponseEntity<>(excelGeneratorService.buildResponseData(response).getBytes(), headers,
					HttpStatus.OK);
		}

		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	@GetMapping("/exportSummaryFinanceExcel")
	public ResponseEntity<byte[]> exportSummaryFinanceExcel(@RequestParam("id") String id,
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccessControlExposeHeaders(ExcelGeneratorService.ACCESS_HEADERS);
		byte[] body = null;

		try {
			Map<String, Object> title = eventService.findIdAndNameByUuid(id);
			String eventName = String.valueOf(title.get("name"));
			List<Map<String, Object>> dataList = summaryReportService.getSummarizeOrderFinance(id, eventName, startDate,
					endDate);
			body = excelGeneratorService.generateCustomExcelForMultipleSheets(dataList);

			String encodedTitle = UriUtils.encode(eventName, StandardCharsets.UTF_8);

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename("Summary_Finance_" + encodedTitle + ".xlsx").build();
			headers.setContentDisposition(contentDisposition);

		} catch (Exception ex) {
			headers.setContentType(MediaType.APPLICATION_JSON);
			Response<Object> response = Response.builder().success(false).message("An error occurred").data(ex.getMessage())
					.build();
			return new ResponseEntity<>(excelGeneratorService.buildResponseData(response).getBytes(), headers,
					HttpStatus.OK);
		}

		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	@GetMapping("/exportSummaryRegistrantExcel")
	public ResponseEntity<byte[]> exportSummaryRegistrantExcel(
			@RequestParam(value = "id", required = false) String id,
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccessControlExposeHeaders(ExcelGeneratorService.ACCESS_HEADERS);
		byte[] body = null;

		try {
			Map<String, Object> title = eventService.findIdAndNameByUuid(id);
			String eventName = String.valueOf(title.get("name"));
			List<Map<String, Object>> dataList = summaryReportService.getSummarizeOrderRegistrant(id, eventName, startDate,
					endDate);
			body = excelGeneratorService.generateCustomExcelForMultipleSheets(dataList);

			String encodedTitle = UriUtils.encode(eventName, StandardCharsets.UTF_8);

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename("Summary_Registrant_" + encodedTitle + ".xlsx").build();
			headers.setContentDisposition(contentDisposition);

		} catch (Exception ex) {
			headers.setContentType(MediaType.APPLICATION_JSON);
			Response<Object> response = Response.builder().success(false).message("An error occurred").data(ex.getMessage())
					.build();
			return new ResponseEntity<>(excelGeneratorService.buildResponseData(response).getBytes(), headers,
					HttpStatus.OK);
		}

		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	@GetMapping("/exportSummaryRevenueExcel")
	public ResponseEntity<byte[]> exportSummaryRevenueExcel(
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccessControlExposeHeaders(ExcelGeneratorService.ACCESS_HEADERS);
		byte[] body = null;

		try {
			List<Map<String, Object>> dataList = summaryReportService.getSummarizeRevenue(startDate, endDate);
			body = excelGeneratorService.generateCustomExcelForMultipleSheets(dataList);

			String yearMonth = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename("Summary_Revenue_" + yearMonth + ".xlsx").build();
			headers.setContentDisposition(contentDisposition);

		} catch (Exception ex) {
			headers.setContentType(MediaType.APPLICATION_JSON);
			Response<Object> response = Response.builder().success(false).message("An error occurred").data(ex.getMessage())
					.build();
			return new ResponseEntity<>(excelGeneratorService.buildResponseData(response).getBytes(), headers,
					HttpStatus.OK);
		}

		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	@GetMapping("/exportSummaryRevenueDetailExcel")
	public ResponseEntity<byte[]> exportSummaryRevenueDetailExcel(
			@RequestParam(value = "id", required = false) String id,
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccessControlExposeHeaders(ExcelGeneratorService.ACCESS_HEADERS);
		byte[] body = null;

		try {
			Map<String, Object> title = eventService.findIdAndNameByUuid(id);
			String eventName = String.valueOf(title.get("name"));
			List<Map<String, Object>> dataList = summaryReportService.getSummarizeRevenueDetail(id, eventName, startDate, endDate);
			body = excelGeneratorService.generateCustomExcelForMultipleSheets(dataList);

			String encodedTitle = UriUtils.encode(eventName, StandardCharsets.UTF_8);

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename("Summary_Revenue_Detail_" + encodedTitle + ".xlsx").build();
			headers.setContentDisposition(contentDisposition);

		} catch (Exception ex) {
			headers.setContentType(MediaType.APPLICATION_JSON);
			Response<Object> response = Response.builder().success(false).message("An error occurred").data(ex.getMessage())
					.build();
			return new ResponseEntity<>(excelGeneratorService.buildResponseData(response).getBytes(), headers,
					HttpStatus.OK);
		}

		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	@GetMapping("/downloadCouponDetails")
	public ResponseEntity<byte[]> downloadCouponDetails(@RequestParam("bucketName") String bucketName) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccessControlExposeHeaders(ExcelGeneratorService.ACCESS_HEADERS);
		byte[] body = null;

		try {
			Map<String, Object> excelData = couponService.getCouponDetailsDownload(bucketName);
			body = excelGeneratorService.generateCustomExcelForMultipleSheets(List.of(excelData));

			String couponName = (String) excelData.get("couponName");
			String encodedTitle = UriUtils.encode(couponName, StandardCharsets.UTF_8);

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename("coupon_details_" + encodedTitle + ".xlsx").build();
			headers.setContentDisposition(contentDisposition);

		} catch (Exception ex) {
			headers.setContentType(MediaType.APPLICATION_JSON);
			Response<Object> response = Response.builder().success(false).message("An error occurred").data(ex.getMessage())
					.build();
			return new ResponseEntity<>(excelGeneratorService.buildResponseData(response).getBytes(), headers,
					HttpStatus.OK);
		}

		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}
}
