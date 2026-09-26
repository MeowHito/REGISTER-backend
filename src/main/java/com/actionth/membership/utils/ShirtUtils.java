package com.actionth.membership.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.OrderDetailShirt;
import com.actionth.membership.model.dto.OrderDetailShirtDto;

/** Read helpers over a runner's garments: the race shirt (legacy columns) plus the extra ones. */
public final class ShirtUtils {

    public static final String RACE = "RACE";

    private ShirtUtils() {
    }

    /** Every garment as a DTO: race shirt first (when picked), then finisher / special ones. */
    public static List<OrderDetailShirtDto> allShirts(OrderDetail od) {
        List<OrderDetailShirtDto> out = new ArrayList<>();
        if (od == null) {
            return out;
        }
        if (od.getShirtType() != null || od.getShirtSize() != null) {
            out.add(OrderDetailShirtDto.builder()
                    .category(RACE)
                    .shirtTypeId(od.getShirtType() != null ? od.getShirtType().getUuid() : null)
                    .shirtTypeName(od.getShirtType() != null ? od.getShirtType().getName() : null)
                    .shirtSizeId(od.getShirtSize() != null ? od.getShirtSize().getUuid() : null)
                    .shirtSizeName(od.getShirtSize() != null ? od.getShirtSize().getName() : null)
                    .build());
        }
        out.addAll(extraShirts(od));
        return out;
    }

    /** Finisher / special shirts only. */
    public static List<OrderDetailShirtDto> extraShirts(OrderDetail od) {
        if (od == null || od.getShirts() == null) {
            return List.of();
        }
        return od.getShirts().stream()
                .filter(s -> !Boolean.FALSE.equals(s.getActive()))
                .map(ShirtUtils::toDto)
                .collect(Collectors.toList());
    }

    public static OrderDetailShirtDto toDto(OrderDetailShirt s) {
        return OrderDetailShirtDto.builder()
                .id(s.getUuid())
                .category(s.getCategory())
                .shirtTypeId(s.getShirtType() != null ? s.getShirtType().getUuid() : null)
                .shirtTypeName(s.getShirtType() != null ? s.getShirtType().getName() : null)
                .shirtSizeId(s.getShirtSize() != null ? s.getShirtSize().getUuid() : null)
                .shirtSizeName(s.getShirtSize() != null ? s.getShirtSize().getName() : null)
                .build();
    }

    /** "Finisher: แขนสั้น / M, VIP: แขนยาว / L" — for Excel cells, emails and logs. */
    public static String describeExtra(OrderDetail od) {
        return extraShirts(od).stream()
                .map(s -> categoryLabel(s.getCategory()) + ": " + (s.getShirtTypeName() != null ? s.getShirtTypeName() : "-")
                        + " / " + (s.getShirtSizeName() != null ? s.getShirtSizeName() : "-"))
                .collect(Collectors.joining(", "));
    }

    public static String categoryLabel(String category) {
        if (category == null) {
            return "เสื้อ";
        }
        return switch (category.toUpperCase()) {
            case "FINISHER" -> "เสื้อ Finisher";
            case "SPECIAL" -> "เสื้อพิเศษ";
            default -> "เสื้อแข่งขัน";
        };
    }
}
