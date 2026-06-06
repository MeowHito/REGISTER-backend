package com.actionth.membership.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.FinanceSummaryDTO;
import com.actionth.membership.model.dto.FinanceSummaryTotalDTO;
import com.actionth.membership.model.dto.PageWithSummary;
import com.actionth.membership.model.dto.RegistrantSummaryDTO;
import com.actionth.membership.model.dto.RevenueDetailSummaryDTO;
import com.actionth.membership.model.dto.RevenueSummaryDTO;

public interface SummaryReportService {

    List<Map<String, Object>> getSummarizeOrderFinance(String eventUuid, String eventName, OffsetDateTime startDate, OffsetDateTime endDate);

    PageWithSummary<FinanceSummaryDTO, FinanceSummaryTotalDTO> getFinanceSummary(String eventUuid, OffsetDateTime startDate, OffsetDateTime endDate, PagingData pagingData);

    Page<RevenueSummaryDTO> getSummarizeOrderRevenue(OffsetDateTime startDate, OffsetDateTime endDate, PagingData pagingData);

    Page<RevenueDetailSummaryDTO> getRevenueDetailSummary(String eventUuid, OffsetDateTime startDate, OffsetDateTime endDate, PagingData pagingData);

    Page<RegistrantSummaryDTO> getSummarizeRegistrant(String eventUuid, OffsetDateTime startDate, OffsetDateTime endDate, PagingData pagingData);

    List<Map<String, Object>> getSummarizeOrderRegistrant(String eventUueventUuid, String eventName, OffsetDateTime startDate, OffsetDateTime endDate);

    List<Map<String, Object>> getSummarizeRevenue(OffsetDateTime startDate, OffsetDateTime endDate);

    List<Map<String, Object>> getSummarizeRevenueDetail(String eventUuid, String eventName, OffsetDateTime startDate, OffsetDateTime endDate);
}
