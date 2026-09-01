package org.example.payment_service.specification;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

public class PaymentSpecification {

    public static Specification<Payment> hasStatus(PaymentStatus paymentStatus) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if(paymentStatus == null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("paymentStatus"), paymentStatus);
        };
    }

    public static Specification<Payment> hasUserPublicId(UUID userPublicId) {
        return (root, query, criteriaBuilder) -> {
            if(userPublicId == null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("userPublicId"), userPublicId);
        };
    }

    public static Specification<Payment> hasOrderPublicId(UUID orderPublicId) {
        return (root, query, criteriaBuilder) ->  {
            if(orderPublicId == null){
                criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("orderPublicId"), orderPublicId);
        };
    }

    public static Specification<Payment> createdBetween(LocalDateTime from, LocalDateTime to) {
        return filterBetweenParams(
                root -> root.get("createdAt"),
                from,
                to
        );
    }

    public static Specification<Payment> updatedBetween(LocalDateTime from, LocalDateTime to) {
        return filterBetweenParams(
                root -> root.get("updatedAt"),
                from,
                to
        );
    }

    public static Specification<Payment> amountBetween(BigDecimal from, BigDecimal to) {
        return filterBetweenParams(
                root -> root.get("amount"),
                from,
                to
        );
    }

    private static <T extends Comparable<? super T>> Specification<Payment> filterBetweenParams(
            Function<Root<Payment>, Path<T>> fieldExtractor,
            T from,
            T to
    ){
        return (
                root,
                query,
                criteriaBuilder
        ) -> {
            if (from == null && to == null) return criteriaBuilder.conjunction();

            Path<T> fieldPath = fieldExtractor.apply(root);

            if (from != null && to != null) {
                return criteriaBuilder.between(fieldPath, from, to);
            }
            if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(fieldPath, from);
            }
            return criteriaBuilder.lessThanOrEqualTo(fieldPath, to);
        };

    }

}
