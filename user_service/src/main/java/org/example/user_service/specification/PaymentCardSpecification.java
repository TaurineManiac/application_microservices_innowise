package org.example.user_service.specification;

import org.example.user_service.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {
    public static Specification<PaymentCard> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if(active ==  null) return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    public static Specification<PaymentCard> hasNumber(String number) {
        return ((root, query, criteriaBuilder) -> {
            if(number == null || number.isEmpty()) return criteriaBuilder.conjunction();
            return criteriaBuilder.like(root.get("number"), "%" + number + "%");
        });
    }

    public static Specification<PaymentCard> hasHolder(String holder) {
        return ((root, query, criteriaBuilder) ->  {
            if(holder == null || holder.isEmpty()) return criteriaBuilder.conjunction();
            return criteriaBuilder.like(root.get("holder"), "%" + holder + "%");
        });
    }

    public static Specification<PaymentCard> belongsToUser (Long userId) {
        return ((root, query, criteriaBuilder) ->  {
            if(userId == null) return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        });
    }

}
