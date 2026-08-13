package org.example.order_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.dto.CreateOrderRequest;
import org.example.order_service.dto.OrderResponse;
import org.example.order_service.dto.UpdateOrderStatusRequest;
import org.example.order_service.enums.OrderStatus;
import org.example.order_service.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("REST request to create order");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request));
    }


    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam(required = false) UUID userPublicId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST request to get orders with filters");
        return ResponseEntity.ok(orderService.getOrders(userPublicId, status, fromDate, toDate, pageable));
    }

    @GetMapping("/user/{userPublicId}")
    public ResponseEntity<Page<OrderResponse>> getOrdersByUser(
            @PathVariable UUID userPublicId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST request to get orders for user: {}", userPublicId);
        return ResponseEntity.ok(orderService.getOrdersByUser(userPublicId, pageable));
    }

    @GetMapping("/{orderPublicId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderPublicId) {
        log.info("REST request to get order by publicId: {}", orderPublicId);
        return ResponseEntity.ok(orderService.getOrderById(orderPublicId));
    }

    @PatchMapping("/{orderPublicId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderPublicId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        log.info("REST request to update status of order {} to {}", orderPublicId, request.getStatus());
        return ResponseEntity.ok(orderService.updateOrder(orderPublicId, request));
    }

    @DeleteMapping("/{orderPublicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderPublicId) {
        log.info("REST request to soft-delete order: {}", orderPublicId);
        orderService.deleteOrder(orderPublicId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{orderPublicId}/revive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reviveOrder(@PathVariable UUID orderPublicId) {
        log.info("REST request to revive order: {}", orderPublicId);
        orderService.reviveOrder(orderPublicId);
        return ResponseEntity.noContent().build();
    }
}