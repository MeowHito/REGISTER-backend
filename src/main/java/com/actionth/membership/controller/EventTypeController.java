package com.actionth.membership.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.response.Response;
import com.actionth.membership.service.EventTypeService;

import com.actionth.membership.model.dto.EventTypeDto;
import com.actionth.membership.model.dto.TeamClubResponse;

@RestController
@RequestMapping("/api/eventType")
public class EventTypeController {

    @Autowired
    private EventTypeService eventTypeService;

    @GetMapping("/{id}")
    public Response<EventTypeDto> getEventTypeById(@PathVariable String id, Boolean active) {
        return new Response<>(eventTypeService.getEventTypeById(id, active), "Event type retrieved successfully", true);
    }

    @GetMapping("/getEventTypeByEventId")
    public Response<List<EventTypeDto>> getEventTypeByEventId(
            @RequestParam("eventId") String eventId,
            @RequestParam(value = "active", required = false) Boolean active) {
        return new Response<>(eventTypeService.getEventTypeByEventId(eventId, active),
                "Event type retrieved successfully", true);
    }

    @GetMapping("/{id}/teamClubs")
    public Response<TeamClubResponse> getTeamClubsByEventType(
            @PathVariable String id,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "limit", required = false, defaultValue = "30") Integer limit) {
        return new Response<>(
                eventTypeService.getTeamClubsByEventType(id, search, page, limit),
                "Team clubs retrieved successfully",
                true);
    }
}
