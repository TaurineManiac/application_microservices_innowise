package org.example.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.client.PaymentProvider;
import org.example.order_service.client.UserInfoProvider;
import org.example.order_service.dto.*;
import org.example.order_service.entity.Item;
import org.example.order_service.entity.Order;
import org.example.order_service.entity.OrderItem;
import org.example.order_service.enums.ItemStatus;
import org.example.order_service.enums.OrderStatus;
import org.example.order_service.exception.AccessDeniedException;
import org.example.order_service.exception.EntityNotFoundException;
import org.example.order_service.mapper.OrderMapper;
import org.example.order_service.repository.ItemRepository;
import org.example.order_service.repository.OrderRepository;
import org.example.order_service.specification.OrderSpecification;
import org.example.order_service.util.SecurityUtils;
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

    private final UserInfoProvider userInfoProvider;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;
    private final PaymentProvider paymentProvider;

    private OrderResponse enrichWithUserInfo(OrderResponse response) {
        UserInfo userInfo = userInfoProvider.getUserInfo(response.getUserPublicId());
        response.setUserInfo(userInfo);
        return response;
    }

    private Specification<Order> buildOrderSpecification(UUID userPublicId, OrderStatus status,
                                                         LocalDateTime fromDate, LocalDateTime toDate) {
        return Specification
                .where(OrderSpecification.notDeleted())
                .and(OrderSpecification.hasUserPublicId(userPublicId))
                .and(OrderSpecification.hasStatus(status))
                .and(OrderSpecification.createdBetween(fromDate, toDate));
    }

    private void checkOrderAccess(Order order) {
        UUID currentUserId = SecurityUtils.getCurrentUserPublicId();
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !order.getUserPublicId().equals(currentUserId)) {
            throw new AccessDeniedException("You don't have permission to access this order");
        }
    }

    private Order orderAvailableByOrderPublicId(UUID orderPublicId) {
        Order order = orderRepository.findByOrderPublicId(orderPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderPublicId));

        if (order.getDeleted()) {
            throw new IllegalStateException("Order is already deleted");
        }
        checkOrderAccess(order);
        return order;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserPublicId();
        log.info("Creating order for user: {}", currentUserId);

        Order order = Order.builder()
                .orderPublicId(UUID.randomUUID())
                .userPublicId(currentUserId)
                .status(OrderStatus.CREATED)
                .price(BigDecimal.ZERO)
                .deleted(false)
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemReq : request.getItems()) {
            Item item = itemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemReq.getItemId()));

            if (item.getStatus() != ItemStatus.ACTIVE) {
                throw new IllegalStateException("Item is not active: " + item.getId());
            }

            OrderItem orderItem = orderItemService.createOrderItem(item, itemReq.getQuantity(), order);
            order.getOrderItems().add(orderItem);

            totalPrice = totalPrice.add(orderItemService.calculateTotalPrice(item, itemReq.getQuantity()));
        }

        order.setPrice(totalPrice);

        Order saved = orderRepository.save(order);
        log.info("Order created with id: {}", saved.getId());

        PaymentRequestEvent paymentRequest = PaymentRequestEvent.builder()
                .orderPublicId(saved.getOrderPublicId())
                .userPublicId(currentUserId)
                .amount(saved.getPrice())
                .build();

        PaymentFullResponseEvent paymentResponse = paymentProvider.createPayment(paymentRequest);
        log.info("Payment initiated: id={}, status={}", paymentResponse.getId(), paymentResponse.getStatus());

        return enrichWithUserInfo(orderMapper.toResponse(saved));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderPublicId) {
        Order order = orderAvailableByOrderPublicId(orderPublicId);
        return enrichWithUserInfo(orderMapper.toResponse(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderFilterRequest filter, Pageable pageable) {
        UUID userPublicId = filter.getUserPublicId();
        OrderStatus status = filter.getStatus();
        LocalDateTime fromDate = filter.getFromDate();
        LocalDateTime toDate = filter.getToDate();

        UUID currentUserId = SecurityUtils.getCurrentUserPublicId();
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isAdmin) {
            if (userPublicId != null && !userPublicId.equals(currentUserId)) {
                throw new AccessDeniedException("You can only view your own orders");
            }
            userPublicId = currentUserId;
        }

        Specification<Order> spec = buildOrderSpecification(userPublicId, status, fromDate, toDate);
        Page<Order> page = orderRepository.findAll(spec, pageable);

        return page.map(orderMapper::toResponse)
                .map(this::enrichWithUserInfo);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(UUID userPublicId, Pageable pageable) {
        OrderFilterRequest filter = new OrderFilterRequest();
        filter.setUserPublicId(userPublicId);
        return getOrders(filter, pageable);
    }

    @Transactional
    public OrderResponse updateOrder(UUID orderPublicId, UpdateOrderStatusRequest request) {
        Order order = orderAvailableByOrderPublicId(orderPublicId);

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
            log.info("Order {} status updated to {}", orderPublicId, request.getStatus());
        }

        Order updated = orderRepository.save(order);
        return enrichWithUserInfo(orderMapper.toResponse(updated));
    }

    @Transactional
    public void deleteOrder(UUID orderPublicId) {
        Order order = orderAvailableByOrderPublicId(orderPublicId);

        order.setDeleted(true);
        orderRepository.save(order);
        log.info("Order {} soft-deleted", orderPublicId);
    }

    @Transactional
    public void reviveOrder(UUID orderPublicId) {
        Order order = orderRepository.findByOrderPublicId(orderPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderPublicId));

        checkOrderAccess(order);

        order.setDeleted(false);
        orderRepository.save(order);
        log.info("Order {} soft-revived", orderPublicId);
    }
}