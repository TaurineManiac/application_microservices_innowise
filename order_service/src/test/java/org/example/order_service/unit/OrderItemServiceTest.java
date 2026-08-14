package org.example.order_service.unit;

import org.example.order_service.entity.Item;
import org.example.order_service.entity.Order;
import org.example.order_service.entity.OrderItem;
import org.example.order_service.enums.ItemStatus;
import org.example.order_service.repository.OrderItemRepository;
import org.example.order_service.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    void createOrderItem_shouldCreateOrderItem() {

        Item item = Item.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("1000.00"))
                .status(ItemStatus.ACTIVE)
                .build();

        Order order = Order.builder()
                .build();

        OrderItem result =
                orderItemService.createOrderItem(
                        item,
                        3,
                        order
                );

        assertThat(result)
                .isNotNull();

        assertThat(result.getItem())
                .isSameAs(item);

        assertThat(result.getOrder())
                .isSameAs(order);

        assertThat(result.getQuantity())
                .isEqualTo(3);

        assertThat(result.getItemsPriceAtMomentOfOrder())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void calculateTotalPrice_shouldCalculateCorrectly() {

        Item item = Item.builder()
                .price(new BigDecimal("125.50"))
                .build();

        BigDecimal result =
                orderItemService.calculateTotalPrice(
                        item,
                        4
                );

        assertThat(result)
                .isEqualByComparingTo("502.00");
    }
}