package com.actionth.membership.controller;

import com.actionth.membership.model.dto.DashboardOrganizerDTO;
import com.actionth.membership.model.dto.DashboardOverviewDTO;
import com.actionth.membership.model.dto.DashboardRegistrationDTO;
import com.actionth.membership.model.dto.EventDto;
import com.actionth.membership.model.dto.QuestionStatsDTO;
import com.actionth.membership.response.Response;

import lombok.RequiredArgsConstructor;

import com.actionth.membership.service.DashboardService;
import com.actionth.membership.service.QuestionStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private QuestionStatsService questionStatsService;

    /**
     * Endpoint สำหรับภาพรวมระบบ
     */
    @GetMapping("/overview/events")
    public Response<List<EventDto>> getAllEvents() {
        List<EventDto> events = dashboardService.getAllEvents();
        return new Response<>(events, "Events retrieved successfully", true);
    }

    @GetMapping("/overview/{eventId}")
    public Response<DashboardOverviewDTO> getDashboardOverview(@PathVariable String eventId,
            @RequestParam(value = "eventTypeId", required = false) String eventTypeId) {
        DashboardOverviewDTO dashboard = dashboardService.getDashboardOverview(eventId, eventTypeId);
        return new Response<>(dashboard, "Dashboard retrieved successfully", true);
    }

    /**
     * Endpoint สำหรับภาพรวมข้อมูลผู้สมัคร
     */
    @GetMapping("/registration/events")
    public Response<List<DashboardOrganizerDTO>> getEventsAndOrganizers() {
        List<DashboardOrganizerDTO> data = dashboardService.getEventsAndOrganizers();
        return new Response<>(data, "Organizers with events retrieved successfully", true);
    }

    @GetMapping("/registration/{eventId}")
    public Response<DashboardRegistrationDTO> getDashboardRegistration(@PathVariable("eventId") String eventId,
            @RequestParam(value = "eventTypeId", required = false) String eventTypeId) {
        DashboardRegistrationDTO dashboardRegistration = dashboardService
                .getDashboardRegistration(eventId, eventTypeId);
        return new Response<>(dashboardRegistration, "Registration Dashboard retrieved successfully", true);
    }

    /** Answers to the event's registration questions (paid participants), for the stats charts. */
    @GetMapping("/questions/{eventId}")
    public Response<QuestionStatsDTO> getQuestionStats(@PathVariable("eventId") String eventId,
            @RequestParam(value = "eventTypeId", required = false) String eventTypeId) {
        return new Response<>(questionStatsService.getStats(eventId, eventTypeId), "Question stats retrieved", true);
    }

}
