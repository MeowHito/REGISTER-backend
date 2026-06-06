package com.actionth.membership.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.actionth.membership.model.EventCalendar;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.EventCalendarDTO;
import com.actionth.membership.service.EmailService;
import com.actionth.membership.service.EventCalendarService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.actionth.membership.response.Response;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eventCalendar")
public class EventCalendarController {

    @Autowired
    private EventCalendarService eventCalendarService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("/getNotiEventCalendar")
    public ResponseEntity<Map<String, Long>> countPendingEvents() {
        long count = eventCalendarService.countPendingEvents();
        return ResponseEntity.ok(Map.of("notiCount", count));
    }

    @GetMapping("/getEventCalendar")
    public Response<Page<EventCalendarDTO>> getAllEventsWithPagination(
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(eventCalendarService.findAll(paging), "Announcements retrieved successfully", true);
    }

    @GetMapping("/{id}")
    public Response<EventCalendarDTO> getEventCalendarById(@PathVariable String id) {
        return new Response<>(eventCalendarService.findByUuid(id), "EventCalendar retrieved successfully", true);
    }

    @PostMapping
    public Response<Void> createEvent(@RequestBody EventCalendarDTO eventCalendarDTO) {
        try {
            eventCalendarService.createEventCalendar(eventCalendarDTO);
            emailService.sendEventCalendarConfirmMail(
                    eventCalendarDTO.getEmail(),
                    eventCalendarDTO.getEventName());
            return new Response<>(null, "Event submitted for approval", true);
        } catch (Exception e) {
            return new Response<>(null, "Failed to submit event", false);
        }
    }

    @PutMapping
    public Response<Void> approveEvent(@RequestBody EventCalendarDTO eventCalendarDTO) {
        try {
            EventCalendar updatedEvent = eventCalendarService.updateApproveStatus(eventCalendarDTO);
            emailService.sendEventCalendarMail(
                    updatedEvent.getEmail(),
                    updatedEvent.getEventName(),
                    updatedEvent.getIsApproved(),
                    updatedEvent.getRejectReason());
            return new Response<>(null, "Event approval status updated", true);
        } catch (Exception e) {
            return new Response<>(null, "Failed to update event approval", false);
        }
    }

    @PutMapping("/update")
    public Response<Void> updateEvent(@RequestBody EventCalendarDTO eventCalendarDTO) {
        try {
            eventCalendarService.updateEventCalendar(eventCalendarDTO);
            return new Response<>(null, "Event updated", true);
        } catch (Exception e) {
            return new Response<>(null, "Failed to update event", false);
        }
    }

    @DeleteMapping
    public Response<Void> deleteEvent(@RequestParam String id) {
        try {
            eventCalendarService.deleteEvent(id);
            return new Response<>(null, "Event deleted successfully", true);
        } catch (Exception e) {
            return new Response<>(null, "Failed to delete event", false);
        }
    }
}
