package org.example.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.entity.Item;
import org.example.order_service.entity.Order;
import org.example.order_service.entity.OrderItem;
import org.example.order_service.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderItem createOrderItem(Item item, Integer quantity, Order order) {
        BigDecimal priceAtMoment = item.getPrice();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .item(item)
                .quantity(quantity)
                .itemsPriceAtMomentOfOrder(priceAtMoment)
                .build();


        log.debug("Created OrderItem for item: {}, quantity: {}, price: {}", item.getId(), quantity, priceAtMoment);
        return orderItem;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPrice(Item item, Integer quantity) {
        return item.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}