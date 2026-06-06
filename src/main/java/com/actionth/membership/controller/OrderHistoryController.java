package com.actionth.membership.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.OrderDetailFullResponse;
import com.actionth.membership.model.dto.OrderHistoryResponse;
import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.model.request.OrderCancelRequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.OrderHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orderHistory")
@RequiredArgsConstructor
public class OrderHistoryController {

    private final OrderHistoryService orderHistoryService;

    private final ObjectMapper mapper;

    @GetMapping
    public Response<Page<OrderHistoryResponse>> getOrderHistory(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(orderHistoryService.search(q, status, startDate, endDate, paging),
                "Order History retrieved successfully", true);
    }

    @GetMapping("/detail")
    public OrderDetailFullResponse getOrderDetail(@RequestParam String orderId) {
        return orderHistoryService.getOrderWithDetails(orderId);
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelOrder(@RequestBody OrderCancelRequest request) {
        orderHistoryService.cancelOrder(request.getOrderId(), request.getCancelledBy());
        return ResponseEntity.ok("Order cancelled successfully");
    }

    @GetMapping("/friends")
    public List<UserDto> getApplicantsWithOrderDetail() {
        return orderHistoryService.getApplicantsWithOrderDetail();
    }

}
