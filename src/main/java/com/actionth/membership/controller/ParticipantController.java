package com.actionth.membership.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.BibDocumentDTO;
import com.actionth.membership.model.dto.ParticipantDTO;
import com.actionth.membership.model.request.ParticipantDTORequest;
import com.actionth.membership.model.request.ParticipantUploadDTORequest;
import com.actionth.membership.model.request.SendBibRequest;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.EmailService;
import com.actionth.membership.service.ParticipantService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/participants")
public class ParticipantController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private AWSService awsService;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public Response<Page<ParticipantDTO>> getParticipantsWithPagination(
            @RequestParam("id") String id,
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(participantService.findAll(id, paging), "Participants retrieved successfully", true);
    }

    @GetMapping("/{id}")
    public Response<ParticipantDTO> getParticipantById(@PathVariable String id) {
        return new Response<>(participantService.findParticipantByUuid(id), "Participants retrieved successfully",
                true);
    }

    @PutMapping("/updateParticipant")
    public Response<Void> updateParticipant(@Valid @RequestBody ParticipantDTORequest participantDTO) {
        participantService.updateParticipant(participantDTO);
        return new Response<>(null, "User updated successfully", true);
    }

    @PutMapping("/uploadParticipant")
    public Response<Void> uploadParticipants(@RequestBody List<ParticipantUploadDTORequest> uploadDTORequests) {
        participantService.uploadParticipants(uploadDTORequests);
        return new Response<>(null, "Participants updated successfully", true);
    }

    @PostMapping("/sendBibByEvent")
    public Response<Void> sendBibDocumentByEvent(@Valid @RequestBody SendBibRequest request) {
        try {
            List<Map<String, Object>> participants = orderDetailRepository.findAllByEventUuid(request.getId());

            if (participants == null || participants.isEmpty()) {
                return new Response<>(null, "ไม่พบผู้เข้าร่วมในอีเวนท์นี้", false);
            }

            Map<String, List<BibDocumentDTO>> emailToBibList = new HashMap<>();

            for (Map<String, Object> row : participants) {
                String email = (String) row.get("email");
                String participantUuid = (String) row.get("participantUuid");
                String prefixPath = (String) row.get("prefixPath");
                String pictureUrl = (String) row.get("pictureUrl");
                String eventKey = (String) row.get("eventKey");

                if (email == null || participantUuid == null)
                    continue;

                String publicUrl = null;
                if (prefixPath != null && !prefixPath.isEmpty() && pictureUrl != null && !pictureUrl.isEmpty()) {
                    try {
                        publicUrl = awsService.getPublicUrl(prefixPath, pictureUrl);
                    } catch (SQLException e) {
                        publicUrl = null;
                    }
                }

                BibDocumentDTO dto = new BibDocumentDTO();
                dto.setParticipantUuid(participantUuid);
                dto.setLogo(publicUrl);
                dto.setEventId(eventKey);

                emailToBibList
                        .computeIfAbsent(email, k -> new ArrayList<>())
                        .add(dto);
            }

            for (Map.Entry<String, List<BibDocumentDTO>> entry : emailToBibList.entrySet()) {
                String email = entry.getKey();
                List<BibDocumentDTO> bibList = entry.getValue();

                emailService.sendBibDocumentsEmail(email, bibList);
            }

            return new Response<>(null, "ส่งอีเมลเรียบร้อยแล้ว", true);

        } catch (Exception e) {
            return new Response<>(null, "เกิดข้อผิดพลาด: " + e.getMessage(), false);
        }
    }
}
