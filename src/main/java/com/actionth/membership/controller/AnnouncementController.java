package com.actionth.membership.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.AnnouncementDTO;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.AnnouncementService;
import com.actionth.membership.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EmailService emailService;

    @Value("${app.announcement-mail-target}")
    private String announcementMailTarget;

    @GetMapping
    public Response<Page<AnnouncementDTO>> getAnnouncementsWithPagination(
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(announcementService.findAll(paging), "Announcements retrieved successfully", true);
    }

    @GetMapping("/{id}")
    public Response<AnnouncementDTO> getAnnouncementById(@PathVariable String id) {
        return new Response<>(announcementService.findByUuid(id), "Announcement retrieved successfully", true);
    }

    @GetMapping("/getNotiAnnouncement")
    public ResponseEntity<Map<String, Long>> countUnreadAnnouncements() {
        long count = announcementService.countUnreadAnnouncements();
        return ResponseEntity.ok(Map.of("notiCount", count));
    }

    @PostMapping
    public Response<Void> createAnnouncement(@RequestBody AnnouncementDTO announcement) {
        try {
            announcementService.createAnnouncement(announcement);

            emailService.sendAnnouncementMail(
                    announcementMailTarget,
                    announcement.getEventName(),
                    announcement.getTitle());

            return new Response<>(null, "Announcement created successfully", true);
        } catch (BusinessException e) {
            return new Response<>(null, e.getMessage(), false);
        } catch (Exception e) {
            return new Response<>(null, "Failed to create announcement", false);
        }
    }

    @PutMapping()
    public Response<Void> updateAnnouncement(@RequestBody AnnouncementDTO announcement) {
        try {
            announcementService.updateAnnouncement(announcement);

            emailService.sendAnnouncementMail(
                    announcementMailTarget,
                    announcement.getEventName(),
                    announcement.getTitle());

            return new Response<>(null, "Announcement updated successfully", true);
        } catch (BusinessException e) {
            return new Response<>(null, e.getMessage(), false);
        } catch (Exception e) {
            return new Response<>(null, "Failed to updated announcement", false);
        }
    }

    @PutMapping("/updateAnnouncementReadStatus")
    public Response<Void> updateAnnouncementReadStatus(@RequestBody AnnouncementDTO announcement) {
        announcementService.updateAnnouncementReadStatus(announcement);
        return new Response<>(null, "Read status updated successfully", true);
    }

    @DeleteMapping("/{uuid}")
    public Response<Void> deleteAnnouncement(@PathVariable String uuid, @RequestParam(value = "mode") String mode) {
        announcementService.deleteAnnouncement(uuid, mode);
        return new Response<>(null, "Announcement deleted successfully", true);
    }
}
