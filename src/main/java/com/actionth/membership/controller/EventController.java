package com.actionth.membership.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.EventDto;
import com.actionth.membership.model.dto.EventSummaryDto;
import com.actionth.membership.model.dto.EventViewDto;
import com.actionth.membership.model.request.GeneralRequest;
import com.actionth.membership.service.EventService;
import com.actionth.membership.service.UserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import com.actionth.membership.response.Response;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @PostMapping("/updateStatus")
    public Response<Void> updateStatus(@RequestBody EventDto dto) {
        eventService.updateStatus(dto);
        return new Response<>(null, "Event created successfully", true);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public Response<EventDto> createEvent(@RequestBody EventDto dto) {
        return new Response<>(eventService.createEvent(dto), "Event created successfully", true);
    }

    @PostMapping("/{uuid}/duplicate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public Response<EventDto> duplicateEvent(@PathVariable String uuid) {
        return new Response<>(eventService.duplicateEvent(uuid), "Event duplicated successfully", true);
    }

    @PutMapping
    public Response<EventDto> updateEvent(@RequestBody EventDto dto) {
        return new Response<>(eventService.updateEvent(dto), "Event updated successfully", true);
    }

    @DeleteMapping("/{uuid}")
    public Response<Void> deleteEvent(@PathVariable String uuid, @RequestParam(value = "mode") String mode) {
        eventService.deleteEvent(uuid, mode);
        return new Response<>(null, "Event deleted successfully", true);
    }

    @GetMapping("/{uuid}")
    public Response<EventDto> getEventByLinkOrUuid(@PathVariable String uuid) {
        return new Response<>(eventService.getEventByLinkOrUuid(uuid), "Events retrieved successfully", true);
    }

    @PostMapping("/getAllEvents")
    public Response<Page<EventViewDto>> getAllEvents(@RequestBody GeneralRequest generalRequest) {
        scopeToCurrentOrganizer(generalRequest);

        return new Response<>(
                eventService.findAll(generalRequest),
                "Events retrieved successfully", true);
    }

    @PostMapping("/summary")
    public Response<EventSummaryDto> getEventSummary(@RequestBody GeneralRequest generalRequest) {
        scopeToCurrentOrganizer(generalRequest);
        return new Response<>(eventService.summarize(generalRequest), "Event summary retrieved successfully", true);
    }

    /** An organizer only ever sees the events they hold a permission on — same rule for list and stats. */
    private void scopeToCurrentOrganizer(GeneralRequest generalRequest) {
        User user = userService.getCurrentUserSession();

        if (user != null && user.getRole() != null) {
            String role = user.getRole().getRole();
            if ("organizer".equals(role)) {
                generalRequest.setCreatedBy(user.getId());
            }
        }
    }

    @GetMapping("/getEventByOrganizer")
    public Response<List<EventDto>> getEventByOrganizer(@RequestParam String id) {
        return new Response<>(eventService.findEventByOrganizer(id), "Event retrieved successfully", true);
    }

    @GetMapping("/getEventByPermission")
    public Response<List<EventDto>> getEventByPermission(@RequestParam String id) {
        return new Response<>(eventService.findEventByPermission(id), "Event retrieved successfully", true);
    }
}
