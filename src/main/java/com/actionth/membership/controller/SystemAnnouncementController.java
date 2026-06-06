package com.actionth.membership.controller;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.SystemAnnouncementDTO;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.SystemAnnouncementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-announcements")
public class SystemAnnouncementController {

    @Autowired
    private SystemAnnouncementService systemAnnouncementService;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping
    public Response<Page<SystemAnnouncementDTO>> getAll(
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(systemAnnouncementService.findAll(paging), "Success", true);
    }

    @GetMapping("/{id}")
    public Response<SystemAnnouncementDTO> getById(@PathVariable String id) {
        return new Response<>(systemAnnouncementService.findByUuid(id), "Success", true);
    }

    @PostMapping
    public Response<Void> create(@RequestBody SystemAnnouncementDTO dto) {
        systemAnnouncementService.create(dto);
        return new Response<>(null, "System announcement created successfully", true);
    }

    @PutMapping
    public Response<Void> update(@RequestBody SystemAnnouncementDTO dto) {
        systemAnnouncementService.update(dto);
        return new Response<>(null, "System announcement updated successfully", true);
    }

    @DeleteMapping("/{uuid}")
    public Response<Void> delete(@PathVariable String uuid) {
        systemAnnouncementService.delete(uuid);
        return new Response<>(null, "System announcement deleted successfully", true);
    }
}
