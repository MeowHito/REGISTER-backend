package com.actionth.membership.model.request;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponDTO {

    private String id;
    private String eventId;
    private String couponName;
    private String bucketName;
    private String couponCode;
    private Long deductionPercentage;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime expiryTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime redeemTime;
    private Integer redeemBy;
    private String redeemByName;
    private Integer limitCoupon;
    private String oldEventId;
    private String oldEventName;
    private String type;
    private String status;
    private Boolean active = true;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<String> runnerIds;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long usedCoupon;
}
