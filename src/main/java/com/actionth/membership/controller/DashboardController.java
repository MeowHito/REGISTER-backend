package com.actionth.membership.controller;

import com.actionth.membership.model.dto.DashboardOrganizerDTO;
import com.actionth.membership.model.dto.DashboardOverviewDTO;
import com.actionth.membership.model.dto.DashboardRegistrationDTO;
import com.actionth.membership.model.dto.EventDto;
import com.actionth.membership.response.Response;

import lombok.RequiredArgsConstructor;

import com.actionth.membership.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * Endpoint สำหรับภาพรวมระบบ
     */
    @GetMapping("/overview/events")
    public Response<List<EventDto>> getAllEvents() {
        List<EventDto> events = dashboardService.getAllEvents();
        return new Response<>(events, "Events retrieved successfully", true);
    }

    @GetMapping("/overview/{eventId}")
    public Response<DashboardOverviewDTO> getDashboardOverview(@PathVariable String eventId) {
        DashboardOverviewDTO dashboard = dashboardService.getDashboardOverview(eventId);
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
    public Response<DashboardRegistrationDTO> getDashboardRegistration(@PathVariable("eventId") String eventId) {
        DashboardRegistrationDTO dashboardRegistration = dashboardService
                .getDashboardRegistration(eventId);
        return new Response<>(dashboardRegistration, "Registration Dashboard retrieved successfully", true);
    }

}
