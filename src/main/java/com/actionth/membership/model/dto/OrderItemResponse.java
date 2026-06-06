package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;

@Data
public class OrderItemResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String idNo;
    private String gender;
    private String nationality;
    private String address;
    private String province;
    private String amphoe;
    private String district;
    private String zipcode;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingAmphoe;
    private String shippingDistrict;
    private String shippingZipcode;
    private String deliveryMethod;
    private String teamClub;
    private Boolean receiveShirt;
    private Boolean rules;
    private Double price;
    private Double netPrice;
    private Double couponDiscount;
    private Double discountShirt;
    private Double shippingFee;
    private Integer pricingId;
    private Integer shirtSizeId;
    private Integer shirtTypeId;
    private String pricingName;
    private String shirtSizeName;
    private String shirtTypeName;
    private String eventTypeName;
    private String ageGroupName;
    private String bibNo;
    private Integer age;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;

    private List<SelectionAnswerDto> selectionAnswers;

}