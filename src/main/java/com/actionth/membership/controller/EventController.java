package com.actionth.membership.controller;

import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.EventDto;
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
    public Response<EventDto> createEvent(@RequestBody EventDto dto) {
        return new Response<>(eventService.createEvent(dto), "Event created successfully", true);
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
        User user = userService.getCurrentUserSession();

        if (user != null && user.getRole() != null) {
            String role = user.getRole().getRole();
            if ("organizer".equals(role)) {
                generalRequest.setCreatedBy(user.getId());
            }
        }

        return new Response<>(
                eventService.findAll(generalRequest),
                "Events retrieved successfully", true);
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
