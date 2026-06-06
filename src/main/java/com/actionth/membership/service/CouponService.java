package com.actionth.membership.service;

import com.actionth.membership.model.Coupon;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.CouponDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CouponService {

    Page<CouponDTO> findAll(PagingData pagingData);

    Page<CouponDTO> findByBucketName(String bucketName, PagingData pagingData);

    Optional<Coupon> findById(Integer id);

    Optional<Coupon> findByUuid(String uuid);

    CouponDTO findFirstByBucketName(String bucketName);

    Coupon save(Coupon coupon);

    void deleteByUuid(String uuid);

    void deleteByUuids(List<String> uuids);

    void deleteByBucketName(String bucketName);

    List<Coupon> createCoupon(CouponDTO couponDTO);

    List<Coupon> updateCoupon(CouponDTO couponDTO);

    List<Coupon> updateCouponStatus(CouponDTO couponDTO);

    List<CouponDTO> getGroupedCouponsByEventIds(List<String> eventIds);
    
    Map<String, Object> validateCoupon(String couponCode, String eventId, List<String> idNos, String orderUuid);

    Map<String, Object> getCouponDetailsDownload(String bucketName);

}
