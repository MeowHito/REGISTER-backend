package com.actionth.membership.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.actionth.membership.model.dto.ResolveReviewResponse;
import com.actionth.membership.model.dto.ReviewContextDto;
import com.actionth.membership.model.dto.ReviewListItemDto;
import com.actionth.membership.model.request.ResolveReviewRequest;
import com.actionth.membership.service.PaymentReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PaymentReviewController {

    private final PaymentReviewService paymentReviewService;

    @GetMapping("/reviews")
    public List<ReviewListItemDto> listReviews() {
        return paymentReviewService.listReviews();
    }

    @GetMapping("/{orderNo}/review-context")
    public ReviewContextDto reviewContext(@PathVariable String orderNo) {
        return paymentReviewService.buildReviewContext(orderNo);
    }

    @PostMapping("/{orderNo}/resolve-review")
    public ResolveReviewResponse resolveReview(@PathVariable String orderNo,
            @RequestBody ResolveReviewRequest request) {
        return paymentReviewService.resolveReview(orderNo, request);
    }
}
