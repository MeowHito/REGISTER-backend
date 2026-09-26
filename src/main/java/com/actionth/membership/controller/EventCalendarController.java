package com.actionth.membership.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.actionth.membership.dto.EventCalendarImportRequest;
import com.actionth.membership.dto.EventCalendarImportStatus;
import com.actionth.membership.job.ImportEventCalendarJob;
import com.actionth.membership.model.EventCalendar;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.EventCalendarDTO;
import com.actionth.membership.service.EmailService;
import com.actionth.membership.service.EventCalendarImportService;
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

    @Autowired
    private EventCalendarImportService eventCalendarImportService;

    @Autowired
    private Scheduler scheduler;

    /** Sync state for the "ดึงจากเว็บอื่น" panel in the back office. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/import/status")
    public Response<EventCalendarImportStatus> getImportStatus() {
        return new Response<>(eventCalendarImportService.getStatus(), "Import status retrieved", true);
    }

    /**
     * Kicks off the import in the background (through Quartz, so it shares the job log and
     * never blocks the request). Poll {@code /import/status} to watch it finish.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import/sync")
    public Response<EventCalendarImportStatus> triggerImport(
            @RequestBody(required = false) EventCalendarImportRequest request) {
        if (eventCalendarImportService.isRunning()) {
            return new Response<>(eventCalendarImportService.getStatus(), "กำลังดึงข้อมูลอยู่ กรุณารอสักครู่", false);
        }
        try {
            JobDataMap data = new JobDataMap();
            if (request != null && request.getHorizonMonths() != null) {
                data.put(ImportEventCalendarJob.DATA_HORIZON, request.getHorizonMonths());
            }
            data.put(ImportEventCalendarJob.DATA_CLEAR_FIRST, request != null && request.isClearFirst());
            scheduler.triggerJob(new JobKey(ImportEventCalendarJob.JOB_NAME), data);
            return new Response<>(eventCalendarImportService.getStatus(), "เริ่มดึงข้อมูลแล้ว", true);
        } catch (Exception e) {
            return new Response<>(null, "ไม่สามารถเริ่มดึงข้อมูลได้: " + e.getMessage(), false);
        }
    }

    /** Stops the run in progress after the item it is on; rows already imported are kept. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import/stop")
    public Response<EventCalendarImportStatus> stopImport() {
        boolean accepted = eventCalendarImportService.requestStop();
        return new Response<>(eventCalendarImportService.getStatus(),
                accepted ? "กำลังหยุดดึงข้อมูล จะหยุดหลังรายการที่กำลังทำอยู่" : "ไม่มีการดึงข้อมูลที่กำลังทำงานอยู่", accepted);
    }

    /** Removes every imported row (manual submissions stay) so the next sync starts from scratch. */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/import")
    public Response<Long> clearImported() {
        try {
            long removed = eventCalendarImportService.clearImported();
            return new Response<>(removed, "ลบข้อมูลที่ดึงมาแล้ว " + removed + " รายการ", true);
        } catch (IllegalStateException e) {
            return new Response<>(null, e.getMessage(), false);
        }
    }

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

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Response<Void> approveEvent(@RequestBody EventCalendarDTO eventCalendarDTO) {
        try {
            EventCalendar updatedEvent = eventCalendarService.updateApproveStatus(eventCalendarDTO);
            // Imported rows have no submitter email — nothing to notify.
            if (updatedEvent.getEmail() != null && !updatedEvent.getEmail().isBlank()) {
                emailService.sendEventCalendarMail(
                        updatedEvent.getEmail(),
                        updatedEvent.getEventName(),
                        updatedEvent.getIsApproved(),
                        updatedEvent.getRejectReason());
            }
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

    @PreAuthorize("hasRole('ADMIN')")
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
