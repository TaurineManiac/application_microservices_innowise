package org.example.user_service.specification;

import org.example.user_service.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasFirstname(String name) {
        return (root, query, criteriaBuilder) ->{
            if(name == null || name.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), '%' + name + '%');
        };
    }

    public static Specification<User> hasSurname(String surname) {
        return ((root, query, criteriaBuilder) -> {
            if(surname == null || surname.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("surname")), "%" + surname + "%");
        });
    }

    public static Specification<User> hasEmail(String email) {
        return ((root, query, criteriaBuilder) ->  {
            if(email == null || email.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + email + "%");
        });
    }

    public static Specification<User> isActive(Boolean active) {
        return ((root, query, criteriaBuilder) ->  {
            if(active == null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        });
    }
}
