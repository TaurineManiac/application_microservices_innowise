package org.example.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.client.UserServiceClient;
import org.example.order_service.dto.CreateOrderItemRequest;
import org.example.order_service.dto.CreateOrderRequest;
import org.example.order_service.dto.OrderResponse;
import org.example.order_service.dto.UpdateOrderRequest;
import org.example.order_service.entity.Item;
import org.example.order_service.entity.Order;
import org.example.order_service.entity.OrderItem;
import org.example.order_service.enums.OrderStatus;
import org.example.order_service.exception.EntityInactiveException;
import org.example.order_service.exception.EntityNotFoundException;
import org.example.order_service.mapper.OrderMapper;
import org.example.order_service.repository.ItemRepository;
import org.example.order_service.repository.OrderRepository;
import org.example.order_service.specification.OrderSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final UserServiceClient userServiceClient;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserPublicId());

        Order order = Order.builder()
                .userPublicId(request.getUserPublicId())
                .status(OrderStatus.CREATED)
                .price(BigDecimal.ZERO)
                .deleted(false)
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemReq : request.getItems()) {
            Item item = itemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemReq.getItemId()));

            OrderItem orderItem = orderItemService.createOrderItem(item, itemReq.getQuantity(), order);
            order.getOrderItems().add(orderItem);

            totalPrice = totalPrice.add(orderItemService.calculateTotalPrice(item, itemReq.getQuantity()));
        }

        order.setPrice(totalPrice);

        Order saved = orderRepository.save(order);
        log.info("Order created with id: {}", saved.getId());

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        if (order.getDeleted()) {
            throw new EntityInactiveException("Order has been deleted");
        }

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(
            UUID userPublicId,
            OrderStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        Specification<Order> spec = Specification
                .where(OrderSpecification.notDeleted())
                .and(OrderSpecification.hasUserPublicId(userPublicId))
                .and(OrderSpecification.hasStatus(status))
                .and(OrderSpecification.createdBetween(fromDate, toDate));

        Page<Order> page = orderRepository.findAll(spec, pageable);
        return page.map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(UUID userPublicId, Pageable pageable) {
        return getOrders(userPublicId, null, null, null, pageable);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        if (order.getDeleted()) {
            throw new IllegalStateException("Cannot update a deleted order");
        }

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
            log.info("Order {} status updated to {}", id, request.getStatus());
        }

        Order updated = orderRepository.save(order);
        return orderMapper.toResponse(updated);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        if (order.getDeleted()) {
            throw new IllegalStateException("Order is already deleted");
        }

        order.setDeleted(true);
        orderRepository.save(order);
        log.info("Order {} soft-deleted", id);
    }

    @Transactional
    public void reviveOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        if (!order.getDeleted()) {
            throw new IllegalStateException("Order is already revived");
        }

        order.setDeleted(false);
        orderRepository.save(order);
        log.info("Order {} soft-revived", id);
    }
}