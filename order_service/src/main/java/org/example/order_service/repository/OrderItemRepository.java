package org.example.order_service.repository;

import org.example.order_service.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph("OrderItem.detail")
    @Override
    Optional<OrderItem> findById(Long id);

    @EntityGraph("OrderItem.detail")
    List<OrderItem> findAllByOrderIdAndOrder_DeletedFalse(Long orderId);

    @EntityGraph("OrderItem.detail")
    Page<OrderItem> findAllByOrderId(Long orderId, Pageable pageable);

    @EntityGraph("OrderItem.detail")
    List<OrderItem> findAllByItemIdAndOrder_DeletedFalse(Long itemId);

    @EntityGraph("OrderItem.detail")
    Page<OrderItem> findAllByItemId(Long itemId, Pageable pageable);

    boolean existsByOrderIdAndItemId(Long orderId, Long itemId);

    long countByOrderId(Long orderId);
}