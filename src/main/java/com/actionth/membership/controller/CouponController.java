package com.actionth.membership.controller;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.CouponDTO;
import com.actionth.membership.model.request.CouponDTORequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.CouponService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public Response<Page<CouponDTO>> getCouponsWithPagination(
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = new ObjectMapper().readValue(pagingJson, PagingData.class);
        }
        return new Response<>(couponService.findAll(paging), "Coupon retrieved successfully", true);
    }

    @GetMapping("/getDetails/{bucketName}")
    public Response<Page<CouponDTO>> getCouponDetailsWithPagination(
            @PathVariable String bucketName,
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = new ObjectMapper().readValue(pagingJson, PagingData.class);
        }
        return new Response<>(couponService.findByBucketName(bucketName, paging), "Coupon retrieved successfully",
                true);
    }

    @GetMapping("/{id}")
    public Response<CouponDTO> getCouponByBucketName(@PathVariable String id) {
        return new Response<>(couponService.findFirstByBucketName(id), "Coupon retrieved successfully", true);
    }

    @PostMapping
    public Response<Void> createCoupon(@RequestBody CouponDTO couponDTO) {
        couponService.createCoupon(couponDTO);
        return new Response<>(null, "Coupon created successfully", true);
    }

    @PostMapping("/getCoupons")
    public ResponseEntity<List<CouponDTO>> getGroupedCoupons(@RequestBody List<String> eventIds) {
        List<CouponDTO> result = couponService.getGroupedCouponsByEventIds(eventIds);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/validateCoupon")
    public ResponseEntity<Map<String, Object>> validateCoupon(@Valid @RequestBody CouponDTORequest request) {
        Map<String, Object> result = couponService.validateCoupon(
            request.getCouponCode(),
            request.getEventId(),
            request.getIdNo(),
            request.getOrderId()
        );

        if ("success".equals(result.get("status"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping
    public Response<Void> updateCoupon(@RequestBody CouponDTO couponDTO) {
        couponService.updateCoupon(couponDTO);
        return new Response<>(null, "Coupon updated successfully", true);
    }

    @PutMapping("/updateCouponStatus")
    public Response<Void> updateCouponStatus(@RequestBody CouponDTO couponDTO) {
        couponService.updateCouponStatus(couponDTO);
        return new Response<>(null, "Coupon updated successfully", true);
    }

    @DeleteMapping
    public Response<Void> deleteCoupon(@RequestBody List<String> ids) {
        couponService.deleteByUuids(ids);
        return new Response<>(null, "Coupon deleted successfully", true);
    }

    @DeleteMapping("/{bucketName}")
    public Response<Void> deleteByBucketName(@PathVariable String bucketName) {
        couponService.deleteByBucketName(bucketName);
        return new Response<>(null, "Coupon deleted successfully", true);
    }
}
