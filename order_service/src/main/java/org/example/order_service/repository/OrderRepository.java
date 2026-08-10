package org.example.order_service.repository;

import org.example.order_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long>, JpaSpecificationExecutor<Order> {
    @EntityGraph("Order.detail")
    @Override
    Optional<Order> findById(Long id);

    @EntityGraph("Order.detail")
    Page<Order> findByUserPublicId(UUID orderId, Pageable pageable,  Specification<Order> spec);

    @EntityGraph("Order.detail")
    @Override
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @EntityGraph("Order.detail")
    Page<Order> findAllByUserPublicIdAndDeletedFalse(UUID userPublicId, Pageable pageable);

    @EntityGraph("Order.detail")
    List<Order> findAllByUserPublicIdAndDeletedFalse(UUID userPublicId);

    Boolean existsByIdAndDeletedFalse(Long id);

}
