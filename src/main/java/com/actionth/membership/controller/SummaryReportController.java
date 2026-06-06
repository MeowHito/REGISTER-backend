package com.actionth.membership.controller;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.FinanceSummaryDTO;
import com.actionth.membership.model.dto.FinanceSummaryTotalDTO;
import com.actionth.membership.model.dto.PageWithSummary;
import com.actionth.membership.model.dto.RegistrantSummaryDTO;
import com.actionth.membership.model.dto.RevenueDetailSummaryDTO;
import com.actionth.membership.model.dto.RevenueSummaryDTO;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.SummaryReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/summaryReport")
public class SummaryReportController {

    private final SummaryReportService summaryReportService;
    private final ObjectMapper mapper;

    @GetMapping("/finace")
    public Response<PageWithSummary<FinanceSummaryDTO, FinanceSummaryTotalDTO>> getFinanceSummary(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "paging", required = false) String pagingJson,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(summaryReportService.getFinanceSummary(id, startDate, endDate, paging),
                "Finance summary retrieved successfully", true);
    }

    @GetMapping("/revenue")
    public Response<Page<RevenueSummaryDTO>> getRevenueSummary(
            @RequestParam(value = "paging", required = false) String pagingJson,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(summaryReportService.getSummarizeOrderRevenue(startDate, endDate, paging),
                "Revenue summary retrieved successfully", true);
    }

    @GetMapping("/revenueDetail")
    public Response<Page<RevenueDetailSummaryDTO>> getRevenueDetailSummary(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "paging", required = false) String pagingJson,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(summaryReportService.getRevenueDetailSummary(id, startDate, endDate, paging),
                "Revenue Detail summary retrieved successfully", true);
    }

    @GetMapping("/registrant")
    public Response<Page<RegistrantSummaryDTO>> getRegistrantSummary(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "paging", required = false) String pagingJson,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(summaryReportService.getSummarizeRegistrant(id, startDate, endDate, paging),
                "Registrant summary retrieved successfully", true);
    }

}
