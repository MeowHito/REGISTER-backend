package com.actionth.membership.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.model.dto.ConfirmationPreviewDto;
import com.actionth.membership.model.dto.PendingConfirmationDto;
import com.actionth.membership.model.dto.ResendResultDto;
import com.actionth.membership.model.request.ResendBulkRequest;
import com.actionth.membership.model.request.ResendConfirmationRequest;
import com.actionth.membership.service.OrderConfirmationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OrderConfirmationController {

    private final OrderConfirmationService orderConfirmationService;

    @GetMapping("/pending-confirmation")
    public List<PendingConfirmationDto> pendingConfirmation() {
        return orderConfirmationService.listMissingConfirmations();
    }

    @GetMapping("/{orderNo}/confirmation-preview")
    public ConfirmationPreviewDto confirmationPreview(@PathVariable String orderNo) {
        return orderConfirmationService.previewConfirmation(orderNo);
    }

    @PostMapping("/{orderNo}/resend-confirmation")
    public ResendResultDto resendConfirmation(@PathVariable String orderNo,
            @RequestBody(required = false) ResendConfirmationRequest request) {
        String testRecipient = request != null ? request.getTestRecipient() : null;
        return orderConfirmationService.resendConfirmation(orderNo, testRecipient);
    }

    @PostMapping("/resend-confirmation")
    public List<ResendResultDto> resendConfirmationBulk(@RequestBody ResendBulkRequest request) {
        return orderConfirmationService.resendBulk(request.getOrderNos());
    }
}
