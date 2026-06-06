package com.actionth.membership.controller;

import com.actionth.membership.model.dto.HelpRequestDto;
import com.actionth.membership.model.request.HelpRequestRequest;
import com.actionth.membership.model.request.HelpRequestStatusRequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.HelpRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/helpRequest")
@RequiredArgsConstructor
public class HelpRequestController {

    private final HelpRequestService helpRequestService;

    @PostMapping
    public ResponseEntity<Response<HelpRequestDto>> createHelpRequest(@RequestBody HelpRequestRequest request) {
        HelpRequestDto result = helpRequestService.createHelpRequest(request);
        return ResponseEntity.ok(Response.<HelpRequestDto>builder()
                .data(result)
                .message("Help request created successfully")
                .success(true)
                .build());
    }

    @GetMapping("/byOrder")
    public ResponseEntity<Response<List<HelpRequestDto>>> getByOrder(@RequestParam String orderUuid) {
        List<HelpRequestDto> result = helpRequestService.getHelpRequestsByOrder(orderUuid);
        return ResponseEntity.ok(Response.<List<HelpRequestDto>>builder()
                .data(result)
                .message("Success")
                .success(true)
                .build());
    }

    @GetMapping
    public ResponseEntity<Response<Page<HelpRequestDto>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<HelpRequestDto> result = helpRequestService.searchAll(status, page, size);
        return ResponseEntity.ok(Response.<Page<HelpRequestDto>>builder()
                .data(result)
                .message("Success")
                .success(true)
                .build());
    }

    @PutMapping("/status")
    public ResponseEntity<Response<HelpRequestDto>> updateStatus(@RequestBody HelpRequestStatusRequest request) {
        HelpRequestDto result = helpRequestService.updateStatus(request);
        return ResponseEntity.ok(Response.<HelpRequestDto>builder()
                .data(result)
                .message("Status updated successfully")
                .success(true)
                .build());
    }
}
