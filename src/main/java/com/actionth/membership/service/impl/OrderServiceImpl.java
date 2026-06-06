package com.actionth.membership.service.impl;

import com.actionth.membership.model.Orders;
import com.actionth.membership.model.PaymentType;
import com.actionth.membership.model.Pricing;
import com.actionth.membership.model.ShirtSize;
import com.actionth.membership.model.ShirtType;
import com.actionth.membership.model.dto.OrderDto;
import com.actionth.membership.model.dto.OrderUpdateResponse;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.model.request.OrderRequest;
import com.actionth.membership.model.request.OrderUpdateRequest;
import com.actionth.membership.model.request.OrderUpdateRequest.RunnerCouponDto;
import com.actionth.membership.model.request.OrderDetailRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.repository.CouponRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.EventTypeRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.repository.OrderRequestLogRepository;
import com.actionth.membership.repository.ShirtTypeRepository;
import com.actionth.membership.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.actionth.membership.model.OrderRequestLog;
import com.actionth.membership.repository.ShirtSizeRepository;
import com.actionth.membership.repository.PricingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.exception.QuotaExceededException;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.Coupon;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.dto.QuotaValidationError;

import com.actionth.membership.service.DistributedLockService;
import com.actionth.membership.utils.AgeGroupUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CouponRepository couponRepository;
    private final ModelMapper modelMapper;

    private final EventTypeRepository eventTypeRepository;
    private final ShirtTypeRepository shirtTypeRepository;
    private final ShirtSizeRepository shirtSizeRepository;
    private final PricingRepository pricingRepository;
    private final EventRepository eventRepository;
    private final DistributedLockService distributedLockService;
    private final OrderRequestLogRepository orderRequestLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new order with quota validation using Redis distributed lock.
     * This prevents race conditions when 100+ requests/sec come in simultaneously.
     * Lock keys are based on pricing UUIDs and event type IDs being ordered.
     */
    @Transactional
    @Override
    public OrderDto createOrder(OrderRequest orderRequest) {
        // Build lock keys from order details
        List<String> lockKeys = buildLockKeys(orderRequest);
        
        // Execute with distributed lock to prevent race conditions
        return distributedLockService.executeWithMultiLock(
            lockKeys,
            5, // wait up to 5 seconds for lock
            30, // hold lock for max 30 seconds
            TimeUnit.SECONDS,
            () -> createOrderInternal(orderRequest)
        );
    }

    /**
     * Build lock keys from order details.
     * Keys are sorted to prevent deadlocks.
     */
    private List<String> buildLockKeys(OrderRequest orderRequest) {
        if (orderRequest.getOrderDetails() == null) {
            return List.of();
        }
        
        return orderRequest.getOrderDetails().stream()
            .filter(req -> req.getEventTypeId() != null)
            .map(req -> {
                if (req.getPricingId() != null) {
                    return "pricing:" + req.getPricingId();
                } else {
                    return "eventtype:" + req.getEventTypeId();
                }
            })
            .distinct()
            .sorted()
            .toList();
    }

    private OrderDto createOrderInternal(OrderRequest orderRequest) {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        String orderNo = null;
        
        log.info("[CreateOrder] correlationId={}, Starting order creation for eventId={}, detailsCount={}",
                correlationId,
                orderRequest.getEventId(),
                orderRequest.getOrderDetails() != null ? orderRequest.getOrderDetails().size() : 0);

        try {
            validateQuotaAvailability(orderRequest);

            Orders order = modelMapper.map(orderRequest, Orders.class);
            order.setPaymentStatus("PENDING");
            orderNo = generateOrderNo();
            order.setOrderNo(orderNo);
            log.info("[CreateOrder] correlationId={}, Generated orderNo={}", correlationId, orderNo);

            String paymentToken = generatePaymentToken(orderNo);
            order.setPaymentToken(paymentToken);

            OffsetDateTime dueAt = OffsetDateTime.now().plusHours(72);

            order.setPaymentDueDatetime(dueAt);
            order.setTokenExpireAt(dueAt);

            Event event = eventRepository.findByLinkOrUuid(orderRequest.getEventId())
                    .orElseThrow(() -> {
                        log.error("[CreateOrder] Event not found: eventId={}", orderRequest.getEventId());
                        return new ResourceNotFoundException("Event not found");
                    });

            order.setEvent(event);
            order.setPaymentDateTime(null);

            if (orderRequest.getOrderDetails() != null && !orderRequest.getOrderDetails().isEmpty()) {
                List<OrderDetail> details = new ArrayList<>();
                for (int i = 0; i < orderRequest.getOrderDetails().size(); i++) {
                    OrderDetailRequest req = orderRequest.getOrderDetails().get(i);
                    OrderDetail orderDetail = modelMapper.map(req, OrderDetail.class);
                    
                    if (req.getBirthDate() != null) {
                        orderDetail.setAge(AgeGroupUtils.calculateAge(req.getBirthDate()));
                    }
                    
                    if (req.getEventTypeId() != null) {
                        final int detailIndex = i;
                        EventType eventType = eventTypeRepository.findByUuid(req.getEventTypeId())
                                .orElseThrow(() -> {
                                    log.error("[CreateOrder] EventType not found: eventTypeId={}, detailIndex={}", 
                                            req.getEventTypeId(), detailIndex);
                                    return new ResourceNotFoundException("EventType not found");
                                });
                        orderDetail.setEventType(eventType);
                    }
                    if (req.getShirtTypeId() != null) {
                        final int detailIndex = i;
                        ShirtType shirtType = shirtTypeRepository.findByUuid(req.getShirtTypeId())
                                .orElseThrow(() -> {
                                    log.error("[CreateOrder] ShirtType not found: shirtTypeId={}, detailIndex={}", 
                                            req.getShirtTypeId(), detailIndex);
                                    return new ResourceNotFoundException("ShirtType not found");
                                });
                        orderDetail.setShirtType(shirtType);
                    }
                    if (req.getShirtSizeId() != null) {
                        final int detailIndex = i;
                        ShirtSize shirtSize = shirtSizeRepository.findByUuid(req.getShirtSizeId())
                                .orElseThrow(() -> {
                                    log.error("[CreateOrder] ShirtSize not found: shirtSizeId={}, detailIndex={}", 
                                            req.getShirtSizeId(), detailIndex);
                                    return new ResourceNotFoundException("ShirtSize not found");
                                });
                        orderDetail.setShirtSize(shirtSize);
                    }
                    if (req.getPricingId() != null) {
                        final int detailIndex = i;
                        Pricing pricing = pricingRepository.findByUuid(req.getPricingId())
                                .orElseThrow(() -> {
                                    log.error("[CreateOrder] Pricing not found: pricingId={}, detailIndex={}", 
                                            req.getPricingId(), detailIndex);
                                    return new ResourceNotFoundException("Pricing not found");
                                });
                        orderDetail.setPricing(pricing);
                    }

                    orderDetail.setOrder(order);
                    orderDetail.setRules(true);
                    details.add(orderDetail);
                }
                order.setOrderDetails(details);
            }

            Orders savedOrder = orderRepository.save(order);
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("[CreateOrder] correlationId={}, Order created successfully: orderNo={}, orderId={}, processingTimeMs={}", 
                    correlationId, savedOrder.getOrderNo(), savedOrder.getId(), processingTime);
            
            saveOrderRequestLog(correlationId, orderRequest, savedOrder.getOrderNo(), "SUCCESS", null, null, processingTime);
            
            return mapOrderToDto(savedOrder);
            
        } catch (QuotaExceededException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.warn("[CreateOrder] correlationId={}, Quota exceeded: eventId={}, error={}", 
                    correlationId, orderRequest.getEventId(), e.getMessage());
            saveOrderRequestLog(correlationId, orderRequest, orderNo, "QUOTA_EXCEEDED", e.getMessage(), "QUOTA_EXCEEDED", processingTime);
            throw e;
        } catch (ResourceNotFoundException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("[CreateOrder] correlationId={}, Resource not found: eventId={}, error={}", 
                    correlationId, orderRequest.getEventId(), e.getMessage());
            saveOrderRequestLog(correlationId, orderRequest, orderNo, "FAILED", e.getMessage(), "RESOURCE_NOT_FOUND", processingTime);
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("[CreateOrder] correlationId={}, Unexpected error creating order: eventId={}, error={}", 
                    correlationId, orderRequest.getEventId(), e.getMessage(), e);
            saveOrderRequestLog(correlationId, orderRequest, orderNo, "FAILED", e.getMessage(), "UNEXPECTED_ERROR", processingTime);
            throw e;
        }
    }

    private void validateQuotaAvailability(OrderRequest orderRequest) {
        if (orderRequest.getOrderDetails() == null || orderRequest.getOrderDetails().isEmpty()) {
            return;
        }

        Map<String, Integer> pricingRequestCount = new HashMap<>();
        Map<Integer, Integer> eventTypeStandardRequestCount = new HashMap<>();

        for (OrderDetailRequest req : orderRequest.getOrderDetails()) {
            if (req.getEventTypeId() == null) {
                continue;
            }

            EventType eventType = eventTypeRepository.findByUuid(req.getEventTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("EventType not found: " + req.getEventTypeId()));

            if (req.getPricingId() != null) {
                pricingRequestCount.merge(req.getPricingId(), 1, Integer::sum);
            } else {
                eventTypeStandardRequestCount.merge(eventType.getId(), 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : pricingRequestCount.entrySet()) {
            String pricingId = entry.getKey();
            Integer requestedCount = entry.getValue();

            Pricing pricing = pricingRepository.findByUuid(pricingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pricing not found: " + pricingId));

            PaymentType paymentType = pricing.getPaymentType();
            String pricingName = paymentType != null ? paymentType.getName() : "Special Price";
            String eventTypeName = pricing.getEventType() != null ? pricing.getEventType().getName() : null;

            if (paymentType != null && paymentType.getEndDate() != null 
                    && paymentType.getEndDate().isBefore(OffsetDateTime.now())) {
                throw new QuotaExceededException(QuotaValidationError.builder()
                        .eventTypeName(eventTypeName)
                        .pricingName(pricingName)
                        .isSpecialPrice(true)
                        .availableQuota(0)
                        .requestedQuota(requestedCount)
                        .errorCode("PRICING_EXPIRED")
                        .message("Pricing '" + pricingName + "' has expired")
                        .build());
            }

            Long usedQuota = orderDetailRepository.countByPricingIdAndPaymentStatus(pricingId);
            Integer availableQuota = pricing.getQuota() - usedQuota.intValue();

            if (availableQuota < requestedCount) {
                throw new QuotaExceededException(QuotaValidationError.builder()
                        .eventTypeName(eventTypeName)
                        .pricingName(pricingName)
                        .isSpecialPrice(true)
                        .availableQuota(Math.max(availableQuota, 0))
                        .requestedQuota(requestedCount)
                        .errorCode("QUOTA_EXCEEDED")
                        .message("Quota for '" + pricingName + "' is not enough")
                        .build());
            }
        }

        for (Map.Entry<Integer, Integer> entry : eventTypeStandardRequestCount.entrySet()) {
            Integer eventTypeId = entry.getKey();
            Integer requestedCount = entry.getValue();

            EventType eventType = eventTypeRepository.findById(eventTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("EventType not found"));

            if (eventType.getQuota() == null) {
                continue;
            }

            Long sumActivePricingQuota = pricingRepository.sumActivePricingQuotaByEventTypeId(eventTypeId);
            Long expiredOrStandardUsed = orderDetailRepository.countByEventTypeIdWithExpiredOrNullPricing(eventTypeId);
            Integer availableQuota = eventType.getQuota() - sumActivePricingQuota.intValue() - expiredOrStandardUsed.intValue();

            if (availableQuota < requestedCount) {
                throw new QuotaExceededException(QuotaValidationError.builder()
                        .eventTypeName(eventType.getName())
                        .pricingName("Standard")
                        .isSpecialPrice(false)
                        .availableQuota(Math.max(availableQuota, 0))
                        .requestedQuota(requestedCount)
                        .errorCode("QUOTA_EXCEEDED")
                        .message("Standard quota for '" + eventType.getName() + "' is not enough")
                        .build());
            }
        }
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String orderNo, String refNo, PaymentStatus paymentStatus) {
        Orders order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (paymentStatus == PaymentStatus.SUCCESS) {
            order.setScbTransactionId(refNo);
            order.setPaymentDateTime(OffsetDateTime.now());
        }

        order.setPaymentStatus(paymentStatus.toString());
        order.setScbTransactionId(refNo);

        orderRepository.save(order);
    }

    @Override
    public Orders findByToken(String token) {
        return orderRepository.findByPaymentToken(token)
                .orElse(null);
    }

    @Override
    public Orders findByUuid(String uuid) {
        return orderRepository.findByUuid(uuid)
                .orElse(null);
    }

    @Override
    public boolean updatePaymentMethod(Integer orderId, String paymentMethod) {
        Optional<Orders> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();
            order.setPaymentMethod(paymentMethod);
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public OrderUpdateResponse updateOrderPayment(OrderUpdateRequest request) {
        String lockKey = "order-update:" + request.getOrderId();
        return distributedLockService.executeWithLock(
            lockKey,
            5,
            30,
            TimeUnit.SECONDS,
            () -> updateOrderPaymentInternal(request)
        );
    }

    private OrderUpdateResponse updateOrderPaymentInternal(OrderUpdateRequest request) {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        String orderNo = null;
        
        log.info("[UpdatePayment] correlationId={}, Starting payment update for orderId={}, couponCode={}", 
                correlationId, request.getOrderId(), request.getCouponCode());
        
        try {
            Orders order = orderRepository.findByUuid(request.getOrderId())
                    .orElseThrow(() -> {
                        log.error("[UpdatePayment] correlationId={}, Order not found: orderId={}", 
                                correlationId, request.getOrderId());
                        return new ResourceNotFoundException("Order not found");
                    });
            
            orderNo = order.getOrderNo();

            String code = request.getCouponCode();
            String codeType = request.getCouponType();
            String previousCoupon = order.getCoupon();

            order.setPaymentMethod(request.getPaymentMethod());
            order.setCouponDiscount(request.getCouponDiscount());
            order.setCoupon(code);
            order.setTotalPrice(request.getTotalPrice());
            order.setFee(request.getFee());
            order.setFeePercent(request.getFeePercent());
            order.setTotalAmountWithFee(request.getTotalAmountWithFee());

            boolean skipPayment = false;
            if (request.getTotalAmountWithFee() != null && request.getTotalAmountWithFee() == 0) {
                skipPayment = validateFreeOrder(order, code, codeType, correlationId);
                if (!skipPayment) {
                    log.warn("[UpdatePayment] correlationId={}, Client sent totalAmountWithFee=0 but server validation failed - rejecting free order, orderNo={}",
                            correlationId, orderNo);
                    throw new IllegalArgumentException(
                            "Invalid free order: server-side validation failed. The coupon does not cover the full order amount.");
                }
            }

            if (skipPayment) {
                log.info("[UpdatePayment] correlationId={}, Total amount is 0 (fully discounted, validated) - marking as SUCCESS, orderNo={}", 
                        correlationId, orderNo);
                order.setPaymentStatus(PaymentStatus.SUCCESS.toString());
                order.setPaymentDateTime(OffsetDateTime.now());
            }

            orderRepository.save(order);
            
            log.debug("[UpdatePayment] correlationId={}, Order payment details updated: orderNo={}, totalAmount={}", 
                    correlationId, order.getOrderNo(), request.getTotalAmountWithFee());

            boolean couponChanged = !Objects.equals(previousCoupon, code);
            if (couponChanged && previousCoupon != null && !previousCoupon.isBlank()) {
                releasePreviousCoupons(order, previousCoupon, correlationId);
            }

            if (code != null && !code.isBlank() && codeType != null && !codeType.isBlank()) {
                log.debug("[UpdatePayment] correlationId={}, Processing coupon: code={}, type={}", 
                        correlationId, code, codeType);
                        
                if (codeType.matches("(?i)internal|external")) {
                    for (RunnerCouponDto rc : request.getRunnerCoupons()) {
                        OrderDetail od = orderDetailRepository.findByOrderIdAndIdNo(order.getId(), rc.getIdNo())
                                .orElseThrow(
                                        () -> {
                                            log.error("[UpdatePayment] correlationId={}, Runner not found: idNo={}", 
                                                    correlationId, rc.getIdNo());
                                            return new IllegalArgumentException("Runner not found in order: " + rc.getIdNo());
                                        });
                        couponRepository.findFirstByCouponCodeAndRedeemByIsNullAndRunnerIdNo(code, rc.getIdNo())
                                .ifPresent(coupon -> {
                                    if (coupon.getRedeemBy() == null) {
                                        coupon.setRedeemBy(od);
                                        coupon.setRedeemTime(OffsetDateTime.now());
                                        couponRepository.save(coupon);
                                        log.info("[UpdatePayment] correlationId={}, Coupon redeemed: code={}, runnerId={}", 
                                                correlationId, code, rc.getIdNo());
                                    }
                                });
                    }
                } else {
                    Optional<Coupon> existing = couponRepository.findByCouponCodeAndRedeemBy_Id(code,
                            order.getOrderDetails().get(0).getId());
                    if (existing.isPresent()) {
                        log.debug("[UpdatePayment] correlationId={}, Coupon already redeemed for this order: code={}", 
                                correlationId, code);
                    } else {
                        couponRepository.findFirstByCouponCodeAndRedeemByIsNull(code).ifPresent(coupon -> {
                            if (coupon.getRedeemBy() == null && order.getOrderDetails() != null
                                    && !order.getOrderDetails().isEmpty()) {
                                coupon.setRedeemBy(order.getOrderDetails().get(0));
                            }
                            if (coupon.getRedeemTime() == null) {
                                coupon.setRedeemTime(OffsetDateTime.now());
                            }
                            couponRepository.save(coupon);
                            log.info("[UpdatePayment] correlationId={}, Coupon redeemed: code={}", 
                                    correlationId, code);
                        });
                    }
                }
            }

            if (request.getRunnerCoupons() != null && !request.getRunnerCoupons().isEmpty()) {
                for (RunnerCouponDto rc : request.getRunnerCoupons()) {
                    OrderDetail od = orderDetailRepository.findByOrderIdAndIdNo(order.getId(), rc.getIdNo())
                            .orElseThrow(() -> {
                                log.error("[UpdatePayment] correlationId={}, Runner not found for discount update: idNo={}", 
                                        correlationId, rc.getIdNo());
                                return new IllegalArgumentException("Runner not found in order: " + rc.getIdNo());
                            });
                    od.setCouponDiscount(rc.getCouponDiscount());
                    od.setCouponUsed(rc.getCouponDiscount() != null && rc.getCouponDiscount() > 0);
                    od.setNetPrice(rc.getNetPrice());
                    orderDetailRepository.save(od);
                }
                log.debug("[UpdatePayment] correlationId={}, Runner discounts updated: runnersCount={}", 
                        correlationId, request.getRunnerCoupons().size());
            } else if (couponChanged && (code == null || code.isBlank()) && order.getOrderDetails() != null) {
                for (OrderDetail od : order.getOrderDetails()) {
                    od.setCouponDiscount(0.0);
                    od.setCouponUsed(false);
                    od.setNetPrice(od.getPrice());
                    orderDetailRepository.save(od);
                }
                log.info("[UpdatePayment] correlationId={}, Coupon removed with empty runnerCoupons — reset {} detail rows", 
                        correlationId, order.getOrderDetails().size());
            }
            

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("[UpdatePayment] correlationId={}, Payment update completed successfully: orderNo={}, skipPayment={}, processingTimeMs={}", 
                    correlationId, orderNo, skipPayment, processingTime);
            
            saveUpdatePaymentLog(correlationId, request, orderNo, "SUCCESS", null, null, processingTime);
            
            if (skipPayment) {
                return OrderUpdateResponse.builder()
                        .skipPayment(true)
                        .message("Order completed without payment (fully discounted)")
                        .build();
            }

            return OrderUpdateResponse.builder()
                    .skipPayment(false)
                    .message("Payment info updated successfully")
                    .build();
                    
        } catch (ResourceNotFoundException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("[UpdatePayment] correlationId={}, Resource not found: orderId={}, error={}, processingTimeMs={}", 
                    correlationId, request.getOrderId(), e.getMessage(), processingTime);
            saveUpdatePaymentLog(correlationId, request, orderNo, "FAILED", e.getMessage(), "RESOURCE_NOT_FOUND", processingTime);
            throw e;
        } catch (IllegalArgumentException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("[UpdatePayment] correlationId={}, Invalid argument: orderId={}, error={}, processingTimeMs={}", 
                    correlationId, request.getOrderId(), e.getMessage(), processingTime);
            saveUpdatePaymentLog(correlationId, request, orderNo, "FAILED", e.getMessage(), "INVALID_ARGUMENT", processingTime);
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("[UpdatePayment] correlationId={}, Unexpected error: orderId={}, error={}, processingTimeMs={}", 
                    correlationId, request.getOrderId(), e.getMessage(), processingTime, e);
            saveUpdatePaymentLog(correlationId, request, orderNo, "FAILED", e.getMessage(), "UNEXPECTED_ERROR", processingTime);
            throw e;
        }
    }

    private boolean validateFreeOrder(Orders order, String couponCode, String couponType, String correlationId) {
        double totalBasePrice = order.getOrderDetails().stream()
                .mapToDouble(od -> od.getPrice() != null ? od.getPrice() : 0.0)
                .sum();

        if (totalBasePrice == 0.0) {
            log.info("[ValidateFreeOrder] correlationId={}, Event is free (totalBasePrice=0), no coupon required, orderNo={}",
                    correlationId, order.getOrderNo());
            return true;
        }

        if (couponCode == null || couponCode.isBlank()) {
            log.warn("[ValidateFreeOrder] correlationId={}, No coupon code provided but totalBasePrice={}, orderNo={}",
                    correlationId, totalBasePrice, order.getOrderNo());
            return false;
        }

        String eventUuid = order.getEvent() != null ? order.getEvent().getUuid() : null;
        if (eventUuid == null) {
            log.warn("[ValidateFreeOrder] correlationId={}, Order has no event, orderNo={}",
                    correlationId, order.getOrderNo());
            return false;
        }

        List<Coupon> coupons = couponRepository.findAllByCouponCodeAndEventIdAndStatusAndRedeemByIsNullOrOrderUuid(
                couponCode, eventUuid, "approved", order.getUuid());

        if (coupons.isEmpty()) {
            log.warn("[ValidateFreeOrder] correlationId={}, No valid approved coupon found: code={}, eventUuid={}, orderNo={}",
                    correlationId, couponCode, eventUuid, order.getOrderNo());
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean hasValid100PercentCoupon = coupons.stream().anyMatch(coupon -> {
            if (coupon.getDeductionPercentage() == null || coupon.getDeductionPercentage() != 100L || 
                (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) || 
                (coupon.getExpiryTime() != null && now.isAfter(coupon.getExpiryTime()))) {
                return false;
            }
            return true;
        });

        if (!hasValid100PercentCoupon) {
            log.warn("[ValidateFreeOrder] correlationId={}, No coupon with 100% deduction found or coupon expired: code={}, orderNo={}",
                    correlationId, couponCode, order.getOrderNo());
            return false;
        }

        double expectedCouponDiscount = totalBasePrice;
        double expectedTotalAfterDiscount = totalBasePrice - expectedCouponDiscount;
        double expectedFee = expectedTotalAfterDiscount * (order.getFeePercent() != null ? order.getFeePercent() / 100.0 : 0.0);
        double expectedTotalWithFee = expectedTotalAfterDiscount + expectedFee;

        if (expectedTotalWithFee != 0.0) {
            log.warn("[ValidateFreeOrder] correlationId={}, Recalculated total is not 0: expectedTotalWithFee={}, totalBasePrice={}, orderNo={}",
                    correlationId, expectedTotalWithFee, totalBasePrice, order.getOrderNo());
            return false;
        }

        int runnerCount = order.getOrderDetails() != null ? order.getOrderDetails().size() : 0;
        long availableCoupons = coupons.stream()
                .filter(c -> c.getDeductionPercentage() != null && c.getDeductionPercentage() == 100L)
                .count();

        if (couponType != null && couponType.matches("(?i)internal|external") && availableCoupons < runnerCount) {
                log.warn("[ValidateFreeOrder] correlationId={}, Not enough coupons for all runners: available={}, runners={}, orderNo={}",
                        correlationId, availableCoupons, runnerCount, order.getOrderNo());
                return false;
            }
        

        log.info("[ValidateFreeOrder] correlationId={}, Free order validated successfully: code={}, deduction=100%, orderNo={}",
                correlationId, couponCode, order.getOrderNo());
        return true;
    }

    private void releasePreviousCoupons(Orders order, String previousCouponCode, String correlationId) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return;
        }
        
        List<Integer> detailIds = order.getOrderDetails().stream()
                .map(OrderDetail::getId)
                .toList();
        
        List<Coupon> redeemedCoupons = couponRepository.findByRedeemBy_IdIn(detailIds);
        
        for (Coupon coupon : redeemedCoupons) {
            if (coupon.getCouponCode() != null && coupon.getCouponCode().equals(previousCouponCode)) {
                coupon.setRedeemBy(null);
                coupon.setRedeemTime(null);
                couponRepository.save(coupon);
                log.info("[UpdatePayment] correlationId={}, Released previously redeemed coupon: code={}, couponId={}", 
                        correlationId, previousCouponCode, coupon.getId());
            }
        }
    }

    private OrderDto mapOrderToDto(Orders order) {
        OrderDto dto = modelMapper.map(order, OrderDto.class);
        dto.setId(order.getUuid());
        return dto;
    }

    private String generateOrderNo() {
        String datePart = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String counterKey = "order-no:" + datePart;
        
        long sequence = distributedLockService.incrementAndGet(counterKey);
        
        if (sequence == 1) {
            distributedLockService.setCounterExpiry(counterKey, 25, TimeUnit.HOURS);
        }
        
        return String.format("OR%s%06d", datePart, sequence);
    }

    private String generatePaymentToken(String orderNo) {
        String timestamp = Instant.now().toString();
        String rawToken = orderNo + "_" + timestamp;
        return Base64.getEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    private void saveOrderRequestLog(String correlationId, OrderRequest orderRequest, 
            String orderNo, String status, String errorMessage, String errorCode, long processingTimeMs) {
        try {
            OrderRequestLog logEntry = OrderRequestLog.builder()
                    .requestType("CREATE")
                    .correlationId(correlationId)
                    .orderNo(orderNo)
                    .eventId(orderRequest.getEventId())
                    .detailsCount(orderRequest.getOrderDetails() != null ? orderRequest.getOrderDetails().size() : 0)
                    .requestBody(maskAndSerializeRequest(orderRequest))
                    .status(status)
                    .errorMessage(errorMessage != null ? truncate(errorMessage, 2000) : null)
                    .errorCode(errorCode)
                    .processingTimeMs(processingTimeMs)
                    .build();
            
            orderRequestLogRepository.save(logEntry);
            log.debug("[CreateOrder] correlationId={}, Audit log saved", correlationId);
        } catch (Exception e) {
            log.error("[CreateOrder] correlationId={}, Failed to save audit log: {}", 
                    correlationId, e.getMessage());
        }
    }

    private void saveUpdatePaymentLog(String correlationId, OrderUpdateRequest request, 
            String orderNo, String status, String errorMessage, String errorCode, long processingTimeMs) {
        try {
            OrderRequestLog logEntry = OrderRequestLog.builder()
                    .requestType("UPDATE_PAYMENT")
                    .correlationId(correlationId)
                    .orderNo(orderNo)
                    .eventId(null)
                    .detailsCount(request.getRunnerCoupons() != null ? request.getRunnerCoupons().size() : 0)
                    .requestBody(maskAndSerializeUpdateRequest(request))
                    .status(status)
                    .errorMessage(errorMessage != null ? truncate(errorMessage, 2000) : null)
                    .errorCode(errorCode)
                    .processingTimeMs(processingTimeMs)
                    .build();
            
            orderRequestLogRepository.save(logEntry);
            log.debug("[UpdatePayment] correlationId={}, Audit log saved", correlationId);
        } catch (Exception e) {
            log.error("[UpdatePayment] correlationId={}, Failed to save audit log: {}", 
                    correlationId, e.getMessage());
        }
    }

    private String maskAndSerializeUpdateRequest(OrderUpdateRequest request) {
        try {
            Map<String, Object> masked = new HashMap<>();
            masked.put("orderId", request.getOrderId());
            masked.put("paymentMethod", request.getPaymentMethod());
            masked.put("couponCode", request.getCouponCode());
            masked.put("couponType", request.getCouponType());
            masked.put("couponDiscount", request.getCouponDiscount());
            masked.put("totalPrice", request.getTotalPrice());
            masked.put("fee", request.getFee());
            masked.put("feePercent", request.getFeePercent());
            masked.put("totalAmountWithFee", request.getTotalAmountWithFee());
            
            if (request.getRunnerCoupons() != null) {
                List<Map<String, Object>> maskedRunners = new ArrayList<>();
                for (RunnerCouponDto runner : request.getRunnerCoupons()) {
                    Map<String, Object> maskedRunner = new HashMap<>();
                    maskedRunner.put("idNo", maskIdNo(runner.getIdNo()));
                    maskedRunner.put("couponDiscount", runner.getCouponDiscount());
                    maskedRunner.put("netPrice", runner.getNetPrice());
                    maskedRunners.add(maskedRunner);
                }
                masked.put("runnerCoupons", maskedRunners);
            }
            
            return objectMapper.writeValueAsString(masked);
        } catch (Exception e) {
            log.warn("Failed to serialize update payment request: {}", e.getMessage());
            return "{}";
        }
    }

    private String maskAndSerializeRequest(OrderRequest request) {
        try {
            Map<String, Object> masked = new HashMap<>();
            masked.put("paymentMethod", request.getPaymentMethod());
            masked.put("paymentDueDatetime", request.getPaymentDueDatetime());
            masked.put("refno2", request.getRefno2());
            masked.put("refno3", request.getRefno3());
            masked.put("eventId", request.getEventId());
            masked.put("qty", request.getQty());
            masked.put("unitPrice", request.getUnitPrice());
            masked.put("totalPrice", request.getTotalPrice());
            masked.put("shippingFee", request.getShippingFee());
            masked.put("discountShirt", request.getDiscountShirt());
            masked.put("coupon", request.getCoupon());
            masked.put("couponDiscount", request.getCouponDiscount());
            masked.put("fee", request.getFee());
            masked.put("feePercent", request.getFeePercent());
            masked.put("totalAmountWithFee", request.getTotalAmountWithFee());
            
            if (request.getOrderDetails() != null) {
                List<Map<String, Object>> maskedDetails = new ArrayList<>();
                for (OrderDetailRequest detail : request.getOrderDetails()) {
                    Map<String, Object> maskedDetail = new HashMap<>();
                    maskedDetail.put("isSelf", detail.getIsSelf());
                    maskedDetail.put("eventTypeId", detail.getEventTypeId());
                    maskedDetail.put("pricingId", detail.getPricingId());
                    maskedDetail.put("shirtTypeId", detail.getShirtTypeId());
                    maskedDetail.put("shirtSizeId", detail.getShirtSizeId());
                    maskedDetail.put("price", detail.getPrice());
                    maskedDetail.put("netPrice", detail.getNetPrice());
                    maskedDetail.put("shippingFee", detail.getShippingFee());
                    maskedDetail.put("couponDiscount", detail.getCouponDiscount());
                    maskedDetail.put("deliveryMethod", detail.getDeliveryMethod());
                    maskedDetail.put("gender", detail.getGender());
                    maskedDetail.put("birthDate", detail.getBirthDate());
                    maskedDetail.put("nationality", detail.getNationality());
                    maskedDetail.put("bloodType", detail.getBloodType());
                    maskedDetail.put("healthIssues", detail.getHealthIssues());
                    maskedDetail.put("emergencyRelation", detail.getEmergencyRelation());
                    maskedDetail.put("province", detail.getProvince());
                    maskedDetail.put("amphoe", detail.getAmphoe());
                    maskedDetail.put("district", detail.getDistrict());
                    maskedDetail.put("zipcode", detail.getZipcode());
                    maskedDetail.put("shippingProvince", detail.getShippingProvince());
                    maskedDetail.put("shippingAmphoe", detail.getShippingAmphoe());
                    maskedDetail.put("shippingDistrict", detail.getShippingDistrict());
                    maskedDetail.put("shippingZipcode", detail.getShippingZipcode());
                    maskedDetail.put("selectionAnswers", detail.getSelectionAnswers());
                    maskedDetail.put("pictureUrl", detail.getPictureUrl());
                    maskedDetail.put("couponUsed", detail.getCouponUsed());
                    maskedDetail.put("prefixPath", detail.getPrefixPath());
                    
                    // Mask PII fields
                    maskedDetail.put("firstName", maskName(detail.getFirstName()));
                    maskedDetail.put("lastName", maskName(detail.getLastName()));
                    maskedDetail.put("firstNameEn", maskName(detail.getFirstNameEn()));
                    maskedDetail.put("lastNameEn", maskName(detail.getLastNameEn()));
                    maskedDetail.put("email", maskEmail(detail.getEmail()));
                    maskedDetail.put("phone", maskPhone(detail.getPhone()));
                    maskedDetail.put("idNo", maskIdNo(detail.getIdNo()));
                    maskedDetail.put("emergencyContact", maskName(detail.getEmergencyContact()));
                    maskedDetail.put("emergencyPhone", maskPhone(detail.getEmergencyPhone()));
                    maskedDetail.put("address", maskAddress(detail.getAddress()));
                    maskedDetail.put("shippingAddress", maskAddress(detail.getShippingAddress()));
                    maskedDetail.put("teamClub", detail.getTeamClub());
                    
                    maskedDetails.add(maskedDetail);
                }
                masked.put("orderDetails", maskedDetails);
            }
            
            return objectMapper.writeValueAsString(masked);
        } catch (Exception e) {
            log.warn("Failed to serialize order request: {}", e.getMessage());
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "***";
        if (name.length() == 1) return name.charAt(0) + "**";
        if (name.length() == 2) return name.charAt(0) + "*" + name.charAt(1);
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }

    private String maskAddress(String address) {
        if (address == null || address.isEmpty()) return null;
        if (address.length() <= 10) return "***";
        return address.substring(0, 5) + "***";
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 5) return "***@***.***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***@***.***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }

    private String maskIdNo(String idNo) {
        if (idNo == null || idNo.length() < 4) return "***";
        return idNo.substring(0, 2) + "***" + idNo.substring(idNo.length() - 2);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
