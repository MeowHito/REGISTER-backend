package com.actionth.membership.utils;

import com.actionth.membership.model.OrderAddOn;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.OrderAddOnDto;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AddOnUtils {

    private AddOnUtils() {
        // Utility class
    }

    /**
     * Add-ons that belong to one participant row. A per-applicant add-on is tied
     * to its runner; a per-order add-on (e.g. two hotel rooms) belongs to nobody
     * in particular, so it is shown only on the first applicant of the order —
     * that keeps per-participant views and Excel sums from counting it twice.
     */
    public static List<OrderAddOn> forParticipant(OrderDetail participant) {
        Orders order = participant.getOrder();
        if (order == null || order.getOrderAddOns() == null || order.getOrderAddOns().isEmpty()) {
            return List.of();
        }
        boolean isFirst = isFirstApplicant(participant);
        return order.getOrderAddOns().stream()
                .filter(oa -> !Boolean.FALSE.equals(oa.getActive()))
                .filter(oa -> oa.getOrderDetail() == null
                        ? isFirst
                        : Objects.equals(oa.getOrderDetail().getId(), participant.getId()))
                .sorted(Comparator.comparing(OrderAddOn::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public static boolean isFirstApplicant(OrderDetail participant) {
        Orders order = participant.getOrder();
        if (order == null || order.getOrderDetails() == null) {
            return true;
        }
        return order.getOrderDetails().stream()
                .filter(d -> !Boolean.FALSE.equals(d.getActive()))
                .map(OrderDetail::getId)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .map(firstId -> firstId.equals(participant.getId()))
                .orElse(true);
    }

    public static OrderAddOnDto toDto(OrderAddOn oa) {
        OrderDetail detail = oa.getOrderDetail();
        String applicantName = detail == null ? null
                : (Objects.toString(detail.getFirstName(), "") + " "
                        + Objects.toString(detail.getLastName(), "")).trim();
        return OrderAddOnDto.builder()
                .id(oa.getUuid())
                .addOnId(oa.getAddOn() != null ? oa.getAddOn().getUuid() : null)
                .orderDetailId(detail != null ? detail.getUuid() : null)
                .applicantName(applicantName != null && applicantName.isEmpty() ? null : applicantName)
                .name(oa.getName())
                .nameEn(oa.getNameEn())
                .noteLabel(oa.getAddOn() != null ? oa.getAddOn().getNoteLabel() : null)
                .unitPrice(oa.getUnitPrice())
                .qty(oa.getQty())
                .totalPrice(oa.getTotalPrice())
                .note(oa.getNote())
                .build();
    }
}
