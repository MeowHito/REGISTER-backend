package com.actionth.membership.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.dto.ParticipantDetailDto;
import com.actionth.membership.model.dto.ParticipantViewDto;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.ParticipantService;
import com.actionth.membership.service.ParticipantTokenService;

@RestController
@RequestMapping("/public-api/participants")
public class PublicParticipantController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ParticipantTokenService participantTokenService;

    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/checkName")
    public Response<List<ParticipantViewDto>> checkName(@RequestParam("eventId") String eventId,
            @RequestParam("name") String name) {
        if (!checklistOpen(eventId)) {
            return new Response<>(List.of(), CHECKLIST_CLOSED, false);
        }
        List<ParticipantViewDto> rows = participantService.checkParticipantName(eventId, name);
        rows.forEach(PublicParticipantController::stripPrivate);
        return new Response<>(rows, "Participants retrieved successfully", true);
    }

    @GetMapping("/check")
    public Response<Page<ParticipantViewDto>> check(@RequestParam("eventId") String eventId,
            @RequestParam("name") String name,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (!checklistOpen(eventId)) {
            return new Response<>(Page.empty(), CHECKLIST_CLOSED, false);
        }
        Page<ParticipantViewDto> rows = participantService.checkParticipant(eventId, name, page, size);
        rows.forEach(PublicParticipantController::stripPrivate);
        return new Response<>(rows, "Participants retrieved successfully", true);
    }

    private static final String CHECKLIST_CLOSED = "งานนี้ไม่ได้เปิดให้ตรวจสอบรายชื่อ";

    /** The public name search only works while the organizer has "show checklist" switched on. */
    private boolean checklistOpen(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return false;
        }
        return eventRepository.findByLinkOrUuid(eventKey.trim())
                .map(e -> Boolean.TRUE.equals(e.getShowChecklist()))
                .orElse(false);
    }

    /** Anyone can search by name, so never hand out a runner's birth date here. */
    private static void stripPrivate(ParticipantViewDto dto) {
        dto.setBirthDate(null);
    }

    @GetMapping("/resolve")
    public Response<Map<String, String>> resolveToken(
            @RequestParam("eventId") String eventKey,
            @RequestParam("qr") String token) {

        if (eventKey != null) {
            eventKey = eventKey.trim();
            if (eventKey.isEmpty())
                eventKey = null;
        }
        if (token != null) {
            token = token.trim();
            if (token.isEmpty())
                token = null;
        }

        if (eventKey == null || token == null) {
            return new Response<>(Map.of(), "Bad request", false);
        }

        Event event = eventRepository.findByLinkOrUuid(eventKey)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        String participantId = participantTokenService.resolveParticipantId(event.getUuid(), token);
        return new Response<>(Map.of("participantId", participantId), "OK", true);
    }

    @GetMapping("/detail")
    public Response<ParticipantDetailDto> getParticipantDetail(
            @RequestParam("participantId") String participantId) {
        return new Response<>(participantService.getParticipantDetail(participantId),
                "Participant detail retrieved successfully", true);
    }
}
