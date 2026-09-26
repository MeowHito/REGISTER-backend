package com.actionth.membership.controller;

import org.springframework.security.access.AccessDeniedException;
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
import com.actionth.membership.service.EventAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventAccessService eventAccessService;

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
        EventDto event = eventService.getEventByLinkOrUuid(uuid);
        // A published event is public (the registration page loads it); a draft is
        // only for the organizers who can manage it.
        if (Boolean.TRUE.equals(event.getIsDraft()) && isCurrentUserOrganizer()) {
            eventAccessService.assertCanByEventUuid(event.getId(), EventAccessService.Access.READ);
        }
        return new Response<>(event, "Events retrieved successfully", true);
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

    /**
     * Only an admin sees every event. Anyone else only sees the events they organize or were
     * invited to with read access — same rule for list and stats. The client's createdBy is
     * always overwritten, so it can't be used to widen or borrow someone else's scope.
     */
    private void scopeToCurrentOrganizer(GeneralRequest generalRequest) {
        if (eventAccessService.isCurrentUserAdmin()) {
            generalRequest.setCreatedBy(null);
            return;
        }
        User user = userService.getCurrentUserSession();
        if (user == null || user.getId() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        generalRequest.setCreatedBy(user.getId());
    }

    /** A non-admin may only list their own events, whatever user id they pass. */
    private String scopeUserUuid(String requestedUuid) {
        if (eventAccessService.isCurrentUserAdmin()) {
            return requestedUuid;
        }
        User user = userService.getCurrentUserSession();
        if (user == null || user.getUuid() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return user.getUuid();
    }

    private boolean isCurrentUserOrganizer() {
        User user = userService.getCurrentUserSession();
        return user != null && user.getRole() != null && "organizer".equals(user.getRole().getRole());
    }

    @GetMapping("/getEventByOrganizer")
    public Response<List<EventDto>> getEventByOrganizer(@RequestParam String id) {
        return new Response<>(eventService.findEventByOrganizer(scopeUserUuid(id)), "Event retrieved successfully", true);
    }

    @GetMapping("/getEventByPermission")
    public Response<List<EventDto>> getEventByPermission(@RequestParam String id) {
        return new Response<>(eventService.findEventByPermission(scopeUserUuid(id)), "Event retrieved successfully", true);
    }
}
