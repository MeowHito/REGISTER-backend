package com.actionth.membership.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.EventPermissionDto;
import com.actionth.membership.model.dto.InviteResponseDto;
import com.actionth.membership.model.request.InviteRequest;
import com.actionth.membership.model.request.UpdatePermissionRequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.EventPermissionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventPermission")
@RequiredArgsConstructor
public class EventPermissionController {

    @Autowired
    private EventPermissionService eventPermissionService;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping
    public Response<Page<EventPermissionDto>> getEventPermissionsByEvent(
            @RequestParam("id") String id,
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(eventPermissionService.getEventPermissionsByEvent(id, paging),
                "Event Permissions retrieved successfully", true);
    }

    @PostMapping("/{eventId}")
    public Response<Void> updatePermissions(
            @PathVariable String eventId,
            @RequestBody UpdatePermissionRequest request) {
        eventPermissionService.updatePermissions(eventId, request);
        return new Response<>(null, "Permissions updated successfully", true);
    }

    @PostMapping("/{eventId}/invite")
    public Response<InviteResponseDto> inviteMembers(
            @PathVariable String eventId,
            @RequestBody InviteRequest request) {
        InviteResponseDto result = eventPermissionService.inviteMembers(eventId, request);
        return new Response<>(result, "Invitations processed", true);
    }

    @PostMapping("/invite/accept")
    public Response<String> acceptInvitation(@RequestParam("token") String token) {
        String eventUuid = eventPermissionService.acceptInvitation(token);
        return new Response<>(eventUuid, "Invitation accepted successfully", true);
    }
}
