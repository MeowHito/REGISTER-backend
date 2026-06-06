package com.actionth.membership.specification;

import org.springframework.data.jpa.domain.Specification;

import com.actionth.membership.model.Orders;

import javax.persistence.criteria.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecifications {

    private OrderSpecifications() {
        
    }

    public static Specification<Orders> filter(String orderNo, String status, OffsetDateTime start, OffsetDateTime end) {
        return (Root<Orders> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (orderNo != null && !orderNo.isEmpty()) {
                predicates.add(cb.equal(root.get("orderNo"), orderNo));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), start));
            }

            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), end));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}