package com.actionth.membership.service;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.User;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.dto.OrderDetailFullResponse;
import com.actionth.membership.model.dto.OrderDetailResponse;
import com.actionth.membership.model.dto.OrderHistoryResponse;
import com.actionth.membership.model.dto.OrderItemResponse;
import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.repository.CouponRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.utils.AgeGroupUtils;
import com.actionth.membership.utils.ContextUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CouponRepository couponRepository;
    private final ModelMapper modelMapper;
    private final ContextUtils contextUtils;
    private final UserService userService;

    public Page<OrderHistoryResponse> search(String q, String status,
            OffsetDateTime start, OffsetDateTime end, PagingData pagingData) {

        User user = userService.getCurrentUserSession();

        if (user == null) {
            return Page.empty();
        }

        boolean isAdmin = user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleType());

        Sort sort = Optional.ofNullable(pagingData.getSortField())
                .map(field -> Sort.by(
                        "DESC".equalsIgnoreCase(pagingData.getSortDirection())
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC,
                        field))
                .orElse(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        Specification<Orders> spec = (root, query, cb) -> {
            Join<Orders, Event> event = root.join("event", JoinType.LEFT);
            Join<Event, User> organizer = event.join("organizer", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (!isAdmin && user.getRole() != null) {
                String role = user.getRole().getRole();
                if ("guest".equals(role)) {
                    predicates.add(cb.equal(root.get("createdBy"), user.getId()));
                } else if ("organizer".equals(role)) {
                    Predicate isOwner = cb.equal(organizer.get("id"), user.getId());

                    Subquery<Integer> epSub = query.subquery(Integer.class);
                    Root<EventPermission> epRoot = epSub.from(EventPermission.class);
                    epSub.select(epRoot.get("event").get("id"))
                            .where(
                                    cb.equal(epRoot.get("user").get("id"), user.getId()),
                                    cb.isTrue(epRoot.get("active")));
                    Predicate hasPermission = event.get("id").in(epSub);

                    predicates.add(cb.or(isOwner, hasPermission));
                }
            }

            if (q != null && !q.isBlank()) {
                String keyword = "%" + q.trim().toLowerCase() + "%";

                Predicate byOrderNo = cb.like(cb.lower(root.get("orderNo")), keyword);
                Predicate byEventName = cb.like(cb.lower(event.get("name")), keyword);

                predicates.add(cb.or(byOrderNo, byEventName));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("paymentStatus"), status));
            }

            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), start));
            }

            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), end));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Orders> orders = orderRepository.findAll(spec, pageable);
        return orders.map(order -> {
            OrderHistoryResponse dto = new OrderHistoryResponse();
            dto.setId(order.getUuid());
            dto.setOrderNo(order.getOrderNo());
            dto.setStatus(order.getPaymentStatus());
            dto.setCreatedTime(order.getCreatedTime());
            dto.setEventName(order.getEvent() != null ? order.getEvent().getName() : null);
            dto.setEventDate(order.getEvent() != null ? order.getEvent().getEventDate() : null);
            dto.setAmount(order.getTotalPrice());
            dto.setReviewReason(order.getReviewReason());
            return dto;
        });
    }

    public OrderDetailResponse getOrderDetail(Integer orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderDetailResponse res = new OrderDetailResponse();
        res.setOrderNo(order.getOrderNo());
        res.setStatus(order.getPaymentStatus());
        res.setCreatedTime(order.getCreatedTime());

        List<OrderDetailResponse.OrderItemDto> items = order.getOrderDetails().stream().map(d -> {
            OrderDetailResponse.OrderItemDto dto = new OrderDetailResponse.OrderItemDto();
            dto.setFullName(d.getFirstName() + " " + d.getLastName());
            dto.setEventTypeName(d.getEventType() != null ? d.getEventType().getName() : "");
            dto.setPrice(d.getNetPrice());
            return dto;
        }).toList();

        res.setItems(items);
        return res;
    }

    @Transactional
    public void cancelOrder(String orderId, String cancelledBy) {
        Orders order = orderRepository.findByUuid(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setPaymentStatus(PaymentStatus.CANCELLED.getJson());
        order.setCancelledDateTime(OffsetDateTime.now());
        order.setCancelledBy(cancelledBy);

        orderRepository.save(order);

        if (order.getCoupon() != null && !order.getCoupon().isBlank()) {
            List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
            for (OrderDetail od : details) {
                couponRepository.findByCouponCodeAndRedeemBy_Id(order.getCoupon(), od.getId()).ifPresent(coupon -> {
                    coupon.setRedeemBy(null);
                    coupon.setRedeemTime(null);
                    couponRepository.save(coupon);
                });
            }
            orderDetailRepository.resetCouponUsedByOrderId(order.getId());
        }
    }

    public OrderDetailFullResponse getOrderWithDetails(String orderId) {
        Orders order = orderRepository.findByUuidOrOrderNo(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<OrderDetail> orderDetails = order.getOrderDetails();

        List<OrderItemResponse> detailDtos = orderDetails.stream().map(detail -> {
            OrderItemResponse dto = modelMapper.map(detail, OrderItemResponse.class);
            dto.setId(detail.getUuid());
            dto.setPricingName(detail.getPricing() != null && detail.getPricing().getPaymentType() != null
                    ? detail.getPricing().getPaymentType().getName()
                    : null);
            dto.setShirtSizeName(detail.getShirtSize() != null ? detail.getShirtSize().getName() : null);
            dto.setShirtTypeName(detail.getShirtType() != null ? detail.getShirtType().getName() : null);
            dto.setEventTypeName(detail.getEventType() != null ? detail.getEventType().getName() : null);
            dto.setAgeGroupName(AgeGroupUtils.resolveAgeGroupCode(detail));
            dto.setBibNo(detail.getBibNo());
            return dto;
        }).toList();

        OrderDetailFullResponse response = new OrderDetailFullResponse();
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getPaymentStatus());
        response.setCreatedTime(order.getCreatedTime());
        response.setEventName(order.getEvent() != null ? order.getEvent().getName() : null);
        response.setEventId(order.getEvent() != null ? order.getEvent().getUuid() : null);
        response.setEventLink(order.getEvent() != null ? order.getEvent().getLink() : null);
        response.setEventDate(order.getEvent() != null ? order.getEvent().getEventDate() : null);
        response.setDetails(detailDtos);
        response.setPaymentToken(order.getPaymentToken());
        response.setUuid(order.getUuid());
        response.setPaymentDueDatetime(order.getPaymentDueDatetime());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setOwnerUuid(order.getCreatedBy() != null ? order.getCreatedBy().getUuid() : null);
        response.setReviewReason(order.getReviewReason());

        return response;
    }

    public List<UserDto> getApplicantsWithOrderDetail() {
        List<OrderDetail> users = new ArrayList<>();
        if (contextUtils.getCurrentUserIdOrNull() != null) {
            users = orderDetailRepository
                    .findLastedApplicatsInOrderDetail(contextUtils.getCurrentUserIdOrNull());
        }

        return users.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }

}
