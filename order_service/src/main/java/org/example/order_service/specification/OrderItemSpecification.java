package org.example.order_service.specification;

import org.example.order_service.entity.OrderItem;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class OrderItemSpecification {

    public static Specification<OrderItem> hasOrderId(Long orderId) {
        return (root, query, cb) -> {
            if (orderId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("order").get("id"), orderId);
        };
    }

    public static Specification<OrderItem> hasItemId(Long itemId) {
        return (root, query, cb) -> {
            if (itemId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("item").get("id"), itemId);
        };
    }

    public static Specification<OrderItem> quantityGreaterThan(Integer minQuantity) {
        return (root, query, cb) -> {
            if (minQuantity == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("quantity"), minQuantity);
        };
    }

    public static Specification<OrderItem> priceAtMomentBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return cb.conjunction();
            }
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("itemsPriceAtMomentOfOrder"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("itemsPriceAtMomentOfOrder"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("itemsPriceAtMomentOfOrder"), maxPrice);
        };
    }
}