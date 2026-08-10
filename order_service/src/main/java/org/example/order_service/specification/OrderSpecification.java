package org.example.order_service.specification;

import org.example.order_service.entity.Order;
import org.example.order_service.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrderSpecification {

    public static Specification<Order> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Order> hasUserPublicId(UUID userPublicId) {
        return (root, query, cb) -> {
            if (userPublicId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("userPublicId"), userPublicId);
        };
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}