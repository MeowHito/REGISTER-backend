package com.actionth.membership.service.impl;

import com.actionth.membership.model.Orders;
import com.actionth.membership.model.PaymentType;
import com.actionth.membership.model.Pricing;
import com.actionth.membership.model.ShirtSize;
import com.actionth.membership.model.ShirtType;
import com.actionth.membership.model.dto.OrderDto;
import com.actionth.membership.model.dto.OrderAddOnDto;
import com.actionth.membership.model.dto.OrderUpdateResponse;
import com.actionth.membership.model.EventAddOn;
import com.actionth.membership.model.OrderAddOn;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.OrderDetailShirt;
import com.actionth.membership.model.dto.OrderDetailShirtDto;
import com.actionth.membership.utils.RegistrationFieldConfig;
import java.util.LinkedHashMap;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.model.request.OrderRequest;
import com.actionth.membership.model.request.OrderUpdateRequest;
import com.actionth.membership.model.request.OrderUpdateRequest.RunnerCouponDto;
import com.actionth.membership.model.request.OrderAddOnRequest;
import com.actionth.membership.model.request.OrderDetailRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
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
import com.actionth.membership.repository.EventAddOnRepository;
import com.actionth.membership.repository.OrderAddOnRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.repository.OrderRequestLogRepository;
import com.actionth.membership.repository.ShirtTypeRepository;
import com.actionth.membership.service.CouponService;
import com.actionth.membership.service.OrderService;
import com.actionth.membership.projection.PricingAvailabilityProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.actionth.membership.model.OrderRequestLog;
import com.actionth.membership.repository.ShirtSizeRepository;
import com.actionth.membership.repository.PricingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.actionth.membership.constant.PaymentFee;
import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.exception.BusinessException;
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

    /** paymentMethod stamped on orders completed under an event's test mode, so they are easy to find and purge. */
    public static final String TEST_MODE_PAYMENT_METHOD = "test";

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final EventAddOnRepository eventAddOnRepository;
    private final OrderAddOnRepository orderAddOnRepository;
    private final CouponRepository couponRepository;
    private final CouponService couponService;
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
     * Build lock keys from the order's tickets and add-ons.
     * Keys are sorted to prevent deadlocks.
     */
    private List<String> buildLockKeys(OrderRequest orderRequest) {
        if (orderRequest.getOrderDetails() == null) {
            return List.of();
        }

        Stream<String> detailKeys = orderRequest.getOrderDetails().stream()
            .filter(req -> req.getEventTypeId() != null)
            .map(req -> {
                if (req.getPricingId() != null) {
                    return "pricing:" + req.getPricingId();
                } else {
                    return "eventtype:" + req.getEventTypeId();
                }
            });

        Stream<String> addOnKeys = (orderRequest.getAddOns() != null ? orderRequest.getAddOns() : List.<OrderAddOnRequest>of())
            .stream()
            .filter(req -> req.getAddOnId() != null)
            .map(req -> "addon:" + req.getAddOnId());

        return Stream.concat(detailKeys, addOnKeys)
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
            validateAddOnAvailability(orderRequest);

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
            validateRequiredFields(event, orderRequest);

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
                    attachExtraShirts(orderDetail, req, event);
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
                validateTeams(details);
                order.setOrderDetails(details);
            }

            Orders savedOrder = orderRepository.save(order);

            // The order details now have ids, so a per-applicant add-on can
            // safely reference the runner it belongs to.
            attachAddOns(savedOrder, orderRequest);
            priceOrder(savedOrder, orderRequest, correlationId);
            savedOrder = orderRepository.save(savedOrder);
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

    /**
     * Add-on stock check, run inside the same distributed lock as the ticket
     * quota check so a burst of concurrent buyers cannot oversell the last
     * hotel room. Rejections reuse {@link QuotaExceededException} so the
     * frontend's existing QUOTA_EXCEEDED handling covers add-ons too.
     */
    private void validateAddOnAvailability(OrderRequest orderRequest) {
        List<OrderAddOnRequest> requested = orderRequest.getAddOns();
        if (requested == null || requested.isEmpty()) {
            return;
        }

        Map<String, Integer> qtyByAddOn = new HashMap<>();
        for (OrderAddOnRequest req : requested) {
            if (req.getAddOnId() == null) {
                continue;
            }
            qtyByAddOn.merge(req.getAddOnId(), Math.max(1, req.getQty() != null ? req.getQty() : 1), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : qtyByAddOn.entrySet()) {
            String addOnId = entry.getKey();
            Integer requestedQty = entry.getValue();

            EventAddOn addOn = eventAddOnRepository.findByUuid(addOnId)
                    .orElseThrow(() -> new ResourceNotFoundException("AddOn not found: " + addOnId));

            if (Boolean.FALSE.equals(addOn.getActive())) {
                throw new QuotaExceededException(QuotaValidationError.builder()
                        .eventTypeName(addOn.getName())
                        .pricingName(addOn.getName())
                        .isSpecialPrice(false)
                        .availableQuota(0)
                        .requestedQuota(requestedQty)
                        .errorCode("ADDON_UNAVAILABLE")
                        .message("Add-on '" + addOn.getName() + "' is no longer on sale")
                        .build());
            }

            if (!Boolean.TRUE.equals(addOn.getPerApplicant())
                    && addOn.getMaxPerOrder() != null && requestedQty > addOn.getMaxPerOrder()) {
                throw new QuotaExceededException(QuotaValidationError.builder()
                        .eventTypeName(addOn.getName())
                        .pricingName(addOn.getName())
                        .isSpecialPrice(false)
                        .availableQuota(addOn.getMaxPerOrder())
                        .requestedQuota(requestedQty)
                        .errorCode("ADDON_LIMIT_EXCEEDED")
                        .message("Add-on '" + addOn.getName() + "' is limited to "
                                + addOn.getMaxPerOrder() + " per order")
                        .build());
            }

            if (addOn.getQuota() == null) {
                continue; // unlimited
            }

            Long used = orderAddOnRepository.sumUsedQtyByAddOnUuid(addOnId);
            int available = addOn.getQuota() - used.intValue();
            if (available < requestedQty) {
                throw new QuotaExceededException(QuotaValidationError.builder()
                        .eventTypeName(addOn.getName())
                        .pricingName(addOn.getName())
                        .isSpecialPrice(false)
                        .availableQuota(Math.max(available, 0))
                        .requestedQuota(requestedQty)
                        .errorCode("ADDON_QUOTA_EXCEEDED")
                        .message("Add-on '" + addOn.getName() + "' is sold out")
                        .build());
            }
        }
    }

    /**
     * Turn the requested add-ons into {@link OrderAddOn} rows. Unit prices come
     * from the organizer's configuration, never from the request body, and the
     * order's addOnTotal is recomputed from them.
     */
    private void attachAddOns(Orders order, OrderRequest orderRequest) {
        List<OrderAddOnRequest> requested = orderRequest.getAddOns();
        if (requested == null || requested.isEmpty()) {
            order.setAddOnTotal(0.0);
            return;
        }

        List<OrderDetail> details = order.getOrderDetails();
        List<OrderAddOn> rows = new ArrayList<>();
        double total = 0.0;

        for (OrderAddOnRequest req : requested) {
            if (req.getAddOnId() == null) {
                continue;
            }

            EventAddOn addOn = eventAddOnRepository.findByUuid(req.getAddOnId())
                    .orElseThrow(() -> new ResourceNotFoundException("AddOn not found: " + req.getAddOnId()));

            boolean perApplicant = Boolean.TRUE.equals(addOn.getPerApplicant());
            int qty = perApplicant ? 1 : Math.max(1, req.getQty() != null ? req.getQty() : 1);
            double unitPrice = addOn.getPrice() != null ? addOn.getPrice().doubleValue() : 0.0;
            double lineTotal = unitPrice * qty;

            OrderDetail applicant = null;
            if (perApplicant) {
                Integer idx = req.getApplicantIndex();
                if (idx == null || idx < 0 || idx >= details.size()) {
                    throw new ResourceNotFoundException(
                            "Applicant not found for add-on '" + addOn.getName() + "': index " + idx);
                }
                applicant = details.get(idx);
            }

            OrderAddOn row = new OrderAddOn();
            row.setOrder(order);
            row.setAddOn(addOn);
            row.setOrderDetail(applicant);
            row.setName(addOn.getName());
            row.setNameEn(addOn.getNameEn());
            row.setUnitPrice(unitPrice);
            row.setQty(qty);
            row.setTotalPrice(lineTotal);
            row.setNote(truncate(req.getNote(), 500));
            rows.add(row);

            total += lineTotal;
        }

        order.getOrderAddOns().clear();
        order.getOrderAddOns().addAll(rows);
        order.setAddOnTotal(total);
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

        for (OrderDetailRequest req : orderRequest.getOrderDetails()) {
            if (req.getEventTypeId() != null) {
                assertCurrentPricing(req.getEventTypeId(), req.getPricingId());
            }
        }
    }

    /**
     * The runner pays the price phase that is on sale now (what the availability endpoint offers),
     * or the standard price when no phase is open. Anything else — a closed Early Bird, a later
     * phase, another distance's pricing — is rejected with PRICING_EXPIRED, which the registration
     * page already turns into a "price changed, please choose again" dialog.
     */
    private void assertCurrentPricing(String eventTypeUuid, String requestedPricingId) {
        EventType eventType = eventTypeRepository.findByUuid(eventTypeUuid)
                .orElseThrow(() -> new ResourceNotFoundException("EventType not found: " + eventTypeUuid));
        List<PricingAvailabilityProjection> open = pricingRepository.findAvailablePricingWithQuota(eventType.getId());
        String currentPricingId = open.isEmpty() ? null : open.get(0).getPricingUuid();
        if (Objects.equals(currentPricingId, requestedPricingId)) {
            return;
        }

        String pricingName = requestedPricingId == null ? "Standard"
                : pricingRepository.findByUuid(requestedPricingId)
                        .map(Pricing::getPaymentType)
                        .map(PaymentType::getName)
                        .orElse(null);
        throw new QuotaExceededException(QuotaValidationError.builder()
                .eventTypeName(eventType.getName())
                .pricingName(pricingName)
                .isSpecialPrice(requestedPricingId != null)
                .availableQuota(0)
                .requestedQuota(1)
                .errorCode("PRICING_EXPIRED")
                .message("ราคาที่เลือกไม่ใช่ราคาที่เปิดขายอยู่ในขณะนี้ กรุณาเลือกใหม่")
                .build());
    }

    /**
     * Snapshot what each runner pays from the organizer's configuration; the request's money
     * fields are ignored (only logged when they disagree). Coupon and service fee come later, in
     * updateOrderPayment, once a payment method is chosen. Shipping is charged once per order.
     */
    private void priceOrder(Orders order, OrderRequest request, String correlationId) {
        Event event = order.getEvent();
        BigDecimal eventShippingFee = event.getShippingFee() != null ? event.getShippingFee() : BigDecimal.ZERO;
        BigDecimal registration = BigDecimal.ZERO;
        BigDecimal shipping = BigDecimal.ZERO;
        boolean shippingCharged = false;
        Set<String> chargedTeams = new HashSet<>();

        for (OrderDetail od : order.getOrderDetails()) {
            EventType eventType = od.getEventType();
            if (eventType == null || eventType.getEvent() == null
                    || !Objects.equals(eventType.getEvent().getId(), event.getId())) {
                throw new IllegalArgumentException("ประเภทการแข่งขันไม่ตรงกับงานที่สมัคร");
            }
            Pricing pricing = od.getPricing();
            if (pricing != null && (pricing.getEventType() == null
                    || !Objects.equals(pricing.getEventType().getId(), eventType.getId()))) {
                throw new IllegalArgumentException("ราคาที่เลือกไม่ตรงกับประเภทการแข่งขัน");
            }

            BigDecimal price = pricing != null ? pricing.getPrice() : eventType.getPrice();
            price = price != null ? price : BigDecimal.ZERO;
            if (Boolean.TRUE.equals(eventType.getIsTeam()) && "PER_TEAM".equalsIgnoreCase(eventType.getTeamPricing())
                    && od.getTeamGroup() != null) {
                // A whole-team price is charged once, on the team's first member.
                if (!chargedTeams.add(eventType.getId() + ":" + od.getTeamGroup())) {
                    price = BigDecimal.ZERO;
                }
            }
            BigDecimal detailShipping = BigDecimal.ZERO;
            if (!shippingCharged && "post".equalsIgnoreCase(od.getDeliveryMethod())) {
                detailShipping = eventShippingFee;
                shippingCharged = true;
            }

            od.setPrice(price.doubleValue());
            od.setDiscountShirt(0.0); // the registration flow offers no "no shirt" option
            od.setCouponDiscount(0.0);
            od.setCouponUsed(false);
            od.setShippingFee(detailShipping.doubleValue());
            od.setNetPrice(price.add(detailShipping).doubleValue());

            registration = registration.add(price);
            shipping = shipping.add(detailShipping);
        }

        BigDecimal addOns = BigDecimal.valueOf(order.getAddOnTotal() != null ? order.getAddOnTotal() : 0.0);
        BigDecimal total = registration.add(shipping).add(addOns);

        order.setUnitPrice(registration.doubleValue());
        order.setShippingFee(shipping.doubleValue());
        order.setDiscountShirt(0.0);
        order.setCoupon(null);
        order.setCouponDiscount(0.0);
        order.setFee(0.0);
        order.setFeePercent(0.0);
        order.setTotalPrice(total.doubleValue());
        order.setTotalAmountWithFee(total.doubleValue());

        if (request.getTotalPrice() != null && total.subtract(BigDecimal.valueOf(request.getTotalPrice())).abs()
                .compareTo(new BigDecimal("0.01")) >= 0) {
            log.warn("[CreateOrder] correlationId={}, Client total {} differs from server total {} - using server total",
                    correlationId, request.getTotalPrice(), total);
        }
    }

    /**
     * Finisher / special shirts picked besides the race shirt. Each must be a style of this
     * event with a size of that style; a RACE style sent here is ignored because the race
     * shirt travels on shirtTypeId / shirtSizeId.
     */
    private void attachExtraShirts(OrderDetail orderDetail, OrderDetailRequest req, Event event) {
        if (req.getShirts() == null || req.getShirts().isEmpty()) {
            return;
        }
        if (orderDetail.getShirts() == null) {
            orderDetail.setShirts(new ArrayList<>());
        }
        for (OrderDetailShirtDto dto : req.getShirts()) {
            if (dto == null || dto.getShirtTypeId() == null) {
                continue;
            }
            ShirtType type = shirtTypeRepository.findByUuid(dto.getShirtTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("ShirtType not found: " + dto.getShirtTypeId()));
            if (type.getEvent() == null || !Objects.equals(type.getEvent().getId(), event.getId())) {
                throw new BusinessException("แบบเสื้อที่เลือกไม่ได้อยู่ในงานที่สมัคร");
            }
            String category = EventServiceImpl.normaliseShirtCategory(type.getCategory());
            if ("RACE".equals(category)) {
                continue;
            }
            ShirtSize size = null;
            if (dto.getShirtSizeId() != null) {
                size = shirtSizeRepository.findByUuid(dto.getShirtSizeId())
                        .orElseThrow(() -> new ResourceNotFoundException("ShirtSize not found: " + dto.getShirtSizeId()));
                if (size.getShirtType() == null || !Objects.equals(size.getShirtType().getId(), type.getId())) {
                    throw new BusinessException("ไซส์เสื้อที่เลือกไม่ตรงกับแบบเสื้อ");
                }
            }
            OrderDetailShirt shirt = new OrderDetailShirt();
            shirt.setOrderDetail(orderDetail);
            shirt.setShirtType(type);
            shirt.setShirtSize(size);
            shirt.setCategory(category);
            orderDetail.getShirts().add(shirt);
        }
    }

    /**
     * Team distances: every member carries the team number the frontend assigned, the team has
     * exactly the configured number of members and one team name (copied to every member's
     * teamClub). Individual distances ignore the team number.
     */
    private void validateTeams(List<OrderDetail> details) {
        Map<String, List<OrderDetail>> byTeam = new LinkedHashMap<>();
        for (OrderDetail od : details) {
            EventType et = od.getEventType();
            if (et == null || !Boolean.TRUE.equals(et.getIsTeam())) {
                od.setTeamGroup(null);
                continue;
            }
            if (od.getTeamGroup() == null) {
                throw new BusinessException("ประเภท " + et.getName() + " เป็นการสมัครแบบทีม กรุณาสมัครเป็นทีม");
            }
            if (od.getTeamClub() == null || od.getTeamClub().isBlank()) {
                throw new BusinessException("กรุณาระบุชื่อทีมสำหรับประเภท " + et.getName());
            }
            byTeam.computeIfAbsent(et.getId() + ":" + od.getTeamGroup(), k -> new ArrayList<>()).add(od);
        }
        for (List<OrderDetail> members : byTeam.values()) {
            EventType et = members.get(0).getEventType();
            int size = et.getTeamSize() != null ? et.getTeamSize() : members.size();
            if (members.size() != size) {
                throw new BusinessException("ทีมของประเภท " + et.getName() + " ต้องมีสมาชิก " + size + " คน");
            }
            String name = members.get(0).getTeamClub().trim();
            members.forEach(m -> m.setTeamClub(name));
        }
    }

    /** The organizer's field configuration is enforced here too, not only by the form. */
    private void validateRequiredFields(Event event, OrderRequest request) {
        if (request.getOrderDetails() == null) {
            return;
        }
        Map<String, String> config = RegistrationFieldConfig.resolve(event);
        for (OrderDetailRequest r : request.getOrderDetails()) {
            for (String field : RegistrationFieldConfig.FIELDS) {
                if (RegistrationFieldConfig.isRequired(config, field) && isBlank(fieldValue(r, field))) {
                    throw new BusinessException("กรุณากรอก" + fieldLabel(field) + "ให้ครบทุกคน");
                }
            }
        }
    }

    private static String fieldValue(OrderDetailRequest r, String field) {
        return switch (field) {
            case "pictureUrl" -> r.getPictureUrl();
            case "firstNameEn" -> r.getFirstNameEn();
            case "lastNameEn" -> r.getLastNameEn();
            case "idNo" -> r.getIdNo();
            case "phone" -> r.getPhone();
            case "province" -> r.getProvince();
            case "nationality" -> r.getNationality();
            case "bloodType" -> r.getBloodType();
            case "healthIssues" -> r.getHealthIssues();
            case "emergencyContact" -> r.getEmergencyContact();
            case "emergencyRelation" -> r.getEmergencyRelation();
            case "emergencyPhone" -> r.getEmergencyPhone();
            case "teamClub" -> r.getTeamClub();
            default -> "x";
        };
    }

    private static String fieldLabel(String field) {
        return switch (field) {
            case "pictureUrl" -> "รูปถ่าย";
            case "firstNameEn" -> "ชื่อ (ภาษาอังกฤษ)";
            case "lastNameEn" -> "นามสกุล (ภาษาอังกฤษ)";
            case "idNo" -> "เลขบัตรประชาชน/พาสปอร์ต";
            case "phone" -> "เบอร์โทรศัพท์";
            case "province" -> "จังหวัด";
            case "nationality" -> "สัญชาติ";
            case "bloodType" -> "หมู่เลือด";
            case "healthIssues" -> "ข้อมูลสุขภาพ";
            case "emergencyContact" -> "ผู้ติดต่อฉุกเฉิน";
            case "emergencyRelation" -> "ความสัมพันธ์ผู้ติดต่อฉุกเฉิน";
            case "emergencyPhone" -> "เบอร์โทรฉุกเฉิน";
            case "teamClub" -> "ชื่อทีม/ชมรม";
            default -> field;
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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

            // Public endpoint: never let it rewrite the money or coupon of a paid/closed order.
            if (!PaymentStatus.PENDING.toString().equalsIgnoreCase(order.getPaymentStatus())) {
                throw new IllegalArgumentException("ออเดอร์นี้ไม่อยู่ในสถานะรอชำระเงิน");
            }

            String code = request.getCouponCode() != null && !request.getCouponCode().isBlank()
                    ? request.getCouponCode().trim() : null;
            String previousCoupon = order.getCoupon();

            // Money is recomputed from the prices snapshotted at create time; the request's
            // totals, fee and per-runner discounts are ignored.
            order.setPaymentMethod(request.getPaymentMethod());
            CouponResult coupon = applyCoupon(order, code, correlationId);
            BigDecimal totalAmountWithFee = applyTotals(order, request, correlationId);

            boolean testMode = order.getEvent() != null && Boolean.TRUE.equals(order.getEvent().getTestMode());
            boolean skipPayment = false;
            if (testMode) {
                log.info("[UpdatePayment] correlationId={}, Event is in test mode - skipping payment, orderNo={}",
                        correlationId, orderNo);
                order.setPaymentMethod(TEST_MODE_PAYMENT_METHOD);
                skipPayment = true;
            } else if (totalAmountWithFee.signum() == 0) {
                log.info("[UpdatePayment] correlationId={}, Server total is 0 (free event or full coupon), orderNo={}",
                        correlationId, orderNo);
                skipPayment = true;
            }

            if (skipPayment) {
                log.info("[UpdatePayment] correlationId={}, Skipping payment (testMode={}) - marking as SUCCESS, orderNo={}",
                        correlationId, testMode, orderNo);
                order.setPaymentStatus(PaymentStatus.SUCCESS.toString());
                order.setPaymentDateTime(OffsetDateTime.now());
            }

            orderRepository.save(order);
            
            log.debug("[UpdatePayment] correlationId={}, Order payment details updated: orderNo={}, totalAmount={}", 
                    correlationId, order.getOrderNo(), totalAmountWithFee);

            if (!Objects.equals(previousCoupon, code) && previousCoupon != null && !previousCoupon.isBlank()) {
                releasePreviousCoupons(order, previousCoupon, correlationId);
            }
            if (code != null) {
                redeemCoupons(order, code, coupon, correlationId);
            }
            

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("[UpdatePayment] correlationId={}, Payment update completed successfully: orderNo={}, skipPayment={}, processingTimeMs={}", 
                    correlationId, orderNo, skipPayment, processingTime);
            
            saveUpdatePaymentLog(correlationId, request, orderNo, "SUCCESS", null, null, processingTime);
            
            if (skipPayment) {
                return OrderUpdateResponse.builder()
                        .skipPayment(true)
                        .message(testMode
                                ? "Order completed without payment (test mode)"
                                : "Order completed without payment (fully discounted)")
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

    /** Which runners a coupon discounts, and its type (internal/external coupons are per runner idNo). */
    private record CouponResult(Set<Integer> detailIds, String type) {
        static final CouponResult NONE = new CouponResult(Set.of(), null);
    }

    /**
     * Re-validates the coupon server-side (same rules as /api/coupon/validateCoupon) and writes
     * each runner's discount: deductionPercentage of (price - shirt discount), shipping excluded,
     * rounded half-up to the satang — the same formula RegistrationPayment shows.
     */
    private CouponResult applyCoupon(Orders order, String code, String correlationId) {
        List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : List.of();
        CouponResult result = CouponResult.NONE;
        BigDecimal percent = BigDecimal.ZERO;

        if (code != null) {
            List<String> idNos = details.stream().map(od -> od.getIdNo() != null ? od.getIdNo().trim() : "").toList();
            Map<String, Object> validation = couponService.validateCoupon(code, order.getEvent().getUuid(), idNos,
                    order.getUuid());
            @SuppressWarnings("unchecked")
            Map<String, Boolean> validIdNos = validation.get("idNo") instanceof Map<?, ?> m
                    ? (Map<String, Boolean>) m : Map.of();
            Set<Integer> eligible = new HashSet<>();
            for (OrderDetail od : details) {
                if (od.getIdNo() != null && Boolean.TRUE.equals(validIdNos.get(od.getIdNo().trim()))) {
                    eligible.add(od.getId());
                }
            }
            if (!"success".equals(validation.get("status")) || eligible.isEmpty()) {
                log.warn("[UpdatePayment] correlationId={}, Coupon rejected by server validation: code={}, orderNo={}",
                        correlationId, code, order.getOrderNo());
                throw new IllegalArgumentException("คูปองนี้ใช้ไม่ได้ หรือถูกใช้ไปแล้ว");
            }
            Object deduction = validation.get("deductionPercentage");
            percent = deduction instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
            result = new CouponResult(eligible, (String) validation.get("type"));
        }

        BigDecimal orderDiscount = BigDecimal.ZERO;
        for (OrderDetail od : details) {
            BigDecimal price = money(od.getPrice());
            BigDecimal shirtDiscount = money(od.getDiscountShirt());
            boolean eligible = result.detailIds().contains(od.getId());
            BigDecimal discount = eligible
                    ? price.subtract(shirtDiscount).max(BigDecimal.ZERO).multiply(percent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            od.setCouponDiscount(discount.doubleValue());
            od.setCouponUsed(eligible);
            od.setNetPrice(price.subtract(shirtDiscount).subtract(discount).add(money(od.getShippingFee())).doubleValue());
            orderDiscount = orderDiscount.add(discount);
        }

        order.setCoupon(code);
        order.setCouponDiscount(orderDiscount.doubleValue());
        return result;
    }

    /** Order total, service fee and amount to charge, from the runners' net prices plus add-ons. */
    private BigDecimal applyTotals(Orders order, OrderUpdateRequest request, String correlationId) {
        BigDecimal total = money(order.getAddOnTotal());
        for (OrderDetail od : order.getOrderDetails() != null ? order.getOrderDetails() : List.<OrderDetail>of()) {
            total = total.add(money(od.getNetPrice()));
        }
        BigDecimal feePercent = PaymentFee.percentFor(request.getPaymentMethod());
        BigDecimal fee = PaymentFee.feeOn(total, feePercent);
        BigDecimal totalAmountWithFee = total.add(fee);

        order.setTotalPrice(total.doubleValue());
        order.setFeePercent(feePercent.doubleValue());
        order.setFee(fee.doubleValue());
        order.setTotalAmountWithFee(totalAmountWithFee.doubleValue());

        if (request.getTotalAmountWithFee() != null && totalAmountWithFee
                .subtract(BigDecimal.valueOf(request.getTotalAmountWithFee())).abs()
                .compareTo(new BigDecimal("0.01")) >= 0) {
            log.warn("[UpdatePayment] correlationId={}, Client amount {} differs from server amount {} - charging server amount, orderNo={}",
                    correlationId, request.getTotalAmountWithFee(), totalAmountWithFee, order.getOrderNo());
        }
        return totalAmountWithFee;
    }

    /**
     * Marks coupon rows as used by the runners they discounted. Internal/external coupons are
     * issued per runner idNo; any other coupon code has one row per use, so each discounted runner
     * takes one row.
     */
    private void redeemCoupons(Orders order, String code, CouponResult coupon, String correlationId) {
        boolean perRunner = coupon.type() != null && coupon.type().matches("(?i)internal|external");
        for (OrderDetail od : order.getOrderDetails()) {
            if (!coupon.detailIds().contains(od.getId())) {
                continue;
            }
            if (couponRepository.findByCouponCodeAndRedeemBy_Id(code, od.getId()).isPresent()) {
                continue; // already redeemed by this runner on an earlier attempt
            }
            Optional<Coupon> row = perRunner
                    ? couponRepository.findFirstByCouponCodeAndRedeemByIsNullAndRunnerIdNo(code, od.getIdNo())
                    : couponRepository.findFirstByCouponCodeAndRedeemByIsNull(code);
            row.ifPresent(c -> {
                c.setRedeemBy(od);
                c.setRedeemTime(OffsetDateTime.now());
                couponRepository.save(c);
                log.info("[UpdatePayment] correlationId={}, Coupon redeemed: code={}, orderNo={}, runnerId={}",
                        correlationId, code, order.getOrderNo(), od.getUuid());
            });
        }
    }

    private static BigDecimal money(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
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
        dto.setOrderAddOns(mapOrderAddOns(order));
        return dto;
    }

    private List<OrderAddOnDto> mapOrderAddOns(Orders order) {
        if (order.getOrderAddOns() == null) {
            return List.of();
        }
        return order.getOrderAddOns().stream()
                .map(oa -> {
                    OrderDetail applicant = oa.getOrderDetail();
                    String applicantName = applicant == null ? null
                            : (Objects.toString(applicant.getFirstName(), "") + " "
                                    + Objects.toString(applicant.getLastName(), "")).trim();
                    return OrderAddOnDto.builder()
                            .id(oa.getUuid())
                            .addOnId(oa.getAddOn() != null ? oa.getAddOn().getUuid() : null)
                            .orderDetailId(applicant != null ? applicant.getUuid() : null)
                            .applicantName(applicantName != null && applicantName.isEmpty() ? null : applicantName)
                            .name(oa.getName())
                            .nameEn(oa.getNameEn())
                            .unitPrice(oa.getUnitPrice())
                            .qty(oa.getQty())
                            .totalPrice(oa.getTotalPrice())
                            .note(oa.getNote())
                            .build();
                })
                .toList();
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
