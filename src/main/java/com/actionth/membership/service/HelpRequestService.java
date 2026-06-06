package com.actionth.membership.service;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.HelpRequest;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.HelpRequestDto;
import com.actionth.membership.model.request.HelpRequestRequest;
import com.actionth.membership.model.request.HelpRequestStatusRequest;
import com.actionth.membership.repository.HelpRequestRepository;
import com.actionth.membership.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelpRequestService {

    private final HelpRequestRepository helpRequestRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    public HelpRequestDto createHelpRequest(HelpRequestRequest request) {
        Orders order = orderRepository.findByUuid(request.getOrderUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderUuid()));

        HelpRequest help = new HelpRequest();
        help.setUuid(UUID.randomUUID().toString());
        help.setOrderUuid(request.getOrderUuid());
        help.setMessage(request.getMessage());
        help.setStatus("NEW");
        help.setActive(true);
        help.setAttachmentUrl(request.getAttachmentUrl());

        helpRequestRepository.save(help);

        // Send confirmation email to the requester
        try {
            if (order.getCreatedBy() != null) {
                User requester = order.getCreatedBy();
                String email = requester.getEmail();
                String name = buildName(requester);
                String orderNo = order.getOrderNo();
                emailService.sendHelpRequestConfirmationEmail(email, name, orderNo, request.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send help request confirmation email for order uuid={}", request.getOrderUuid(), e);
        }

        return toDto(help, order);
    }

    public List<HelpRequestDto> getHelpRequestsByOrder(String orderUuid) {
        Orders order = orderRepository.findByUuid(orderUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderUuid));

        return helpRequestRepository.findByOrderUuidAndActiveTrue(orderUuid).stream()
                .map(h -> toDto(h, order))
                .collect(Collectors.toList());
    }

    public Page<HelpRequestDto> searchAll(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTime"));

        Page<HelpRequest> results;
        if (status != null && !status.isBlank()) {
            results = helpRequestRepository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.equal(root.get("status"), status),
                            cb.isTrue(root.get("active"))
                    ),
                    pageable
            );
        } else {
            results = helpRequestRepository.findAll(
                    (root, query, cb) -> cb.isTrue(root.get("active")),
                    pageable
            );
        }

        return results.map(h -> {
            Orders order = orderRepository.findByUuid(h.getOrderUuid()).orElse(null);
            return toDto(h, order);
        });
    }

    public HelpRequestDto updateStatus(HelpRequestStatusRequest request) {
        HelpRequest help = helpRequestRepository.findByUuidAndActiveTrue(request.getUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Help request not found: " + request.getUuid()));

        String previousStatus = help.getStatus();
        help.setStatus(request.getStatus());
        if (request.getAdminNote() != null) {
            help.setAdminNote(request.getAdminNote());
        }
        helpRequestRepository.save(help);

        // Send email notification to the requester if status changed
        if (!previousStatus.equals(request.getStatus())) {
            try {
                Orders order = orderRepository.findByUuid(help.getOrderUuid()).orElse(null);
                if (order != null && order.getCreatedBy() != null) {
                    User requester = order.getCreatedBy();
                    String email = requester.getEmail();
                    String name = buildName(requester);
                    String orderNo = order.getOrderNo();
                    emailService.sendHelpStatusUpdateEmail(email, name, orderNo, request.getStatus(), request.getAdminNote());
                }
            } catch (Exception e) {
                log.error("Failed to send help status update email for help uuid={}", request.getUuid(), e);
            }
        }

        Orders order = orderRepository.findByUuid(help.getOrderUuid()).orElse(null);
        return toDto(help, order);
    }

    private HelpRequestDto toDto(HelpRequest help, Orders order) {
        HelpRequestDto dto = new HelpRequestDto();
        dto.setUuid(help.getUuid());
        dto.setOrderUuid(help.getOrderUuid());
        dto.setMessage(help.getMessage());
        dto.setStatus(help.getStatus());
        dto.setAdminNote(help.getAdminNote());
        dto.setCreatedTime(help.getCreatedTime());
        dto.setUpdatedTime(help.getUpdatedTime());
        dto.setAttachmentUrl(help.getAttachmentUrl());

        if (order != null) {
            dto.setOrderNo(order.getOrderNo());
            if (order.getCreatedBy() != null) {
                dto.setRequesterName(buildName(order.getCreatedBy()));
                dto.setRequesterEmail(order.getCreatedBy().getEmail());
            }
        }
        return dto;
    }

    private String buildName(User user) {
        String name = "";
        if (user.getFirstName() != null) name += user.getFirstName();
        if (user.getLastName() != null) name += " " + user.getLastName();
        return name.trim().isEmpty() ? (user.getEmail() != null ? user.getEmail() : "") : name.trim();
    }
}
