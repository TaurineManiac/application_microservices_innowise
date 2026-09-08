package org.example.order_service.integration;

import org.example.order_service.TestcontainersConfiguration;
import org.example.order_service.client.PaymentProvider;
import org.example.order_service.client.UserInfoProvider;
import org.example.order_service.dto.PaymentFullResponseEvent;
import org.example.order_service.dto.PaymentRequestEvent;
import org.example.order_service.entity.Item;
import org.example.order_service.entity.Order;
import org.example.order_service.entity.OrderItem;
import org.example.order_service.enums.ItemStatus;
import org.example.order_service.enums.OrderStatus;
import org.example.order_service.enums.PaymentStatus;
import org.example.order_service.repository.ItemRepository;
import org.example.order_service.repository.OrderItemRepository;
import org.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private UserInfoProvider userInfoProvider;

    @MockitoBean
    private PaymentProvider paymentProvider;

    private UUID currentUserId;
    private UUID anotherUserId;

    private Item item;

    @BeforeEach
    void setUp() {

        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();

        currentUserId = UUID.randomUUID();
        anotherUserId = UUID.randomUUID();

        item = Item.builder()
                .name("Laptop")
                .price(new BigDecimal("1000.00"))
                .status(ItemStatus.ACTIVE)
                .build();

        item = itemRepository.saveAndFlush(item);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(
                        org.example.order_service.dto.UserInfo.builder()
                                .publicId(currentUserId)
                                .name("John")
                                .surname("Doe")
                                .email("john@test.com")
                                .build()
                );

        when(paymentProvider.createPayment(any(PaymentRequestEvent.class)))
                .thenReturn(PaymentFullResponseEvent.builder()
                        .id(1L)
                        .orderPublicId(UUID.randomUUID())
                        .userPublicId(currentUserId)
                        .amount(new BigDecimal("100.00"))
                        .status(PaymentStatus.COMPLETED)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
    }

    @Test
    void createOrder_shouldCreateOrder() throws Exception {

        String json = """
                {
                    "items": [
                        {
                            "itemId": %d,
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(item.getId());

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(user(currentUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderPublicId").exists())
                .andExpect(jsonPath("$.userPublicId")
                        .value(currentUserId.toString()))
                .andExpect(jsonPath("$.status")
                        .value("CREATED"))
                .andExpect(jsonPath("$.price")
                        .value(2000.00));

        List<Order> orders =
                orderRepository.findAll();

        assertThat(orders)
                .hasSize(1);

        Order savedOrder = orders.get(0);
        assertThat(savedOrder.getUserPublicId())
                .isEqualTo(currentUserId);

        assertThat(savedOrder.getPrice())
                .isEqualByComparingTo("2000.00");

        List<OrderItem> orderItems = orderItemRepository.findAll();
        assertThat(orderItems)
                .hasSize(1);
        OrderItem orderItem = orderItems.get(0);
        assertThat(orderItem.getQuantity())
                .isEqualTo(2);
        assertThat(orderItem.getItem().getId())
                .isEqualTo(item.getId());
        assertThat(orderItem.getItemsPriceAtMomentOfOrder())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void createOrder_shouldCreateMultipleOrderItems() throws Exception {

        Item secondItem = Item.builder()
                .name("Mouse")
                .price(new BigDecimal("50.00"))
                .status(ItemStatus.ACTIVE)
                .build();

        secondItem = itemRepository.saveAndFlush(secondItem);

        String json = """
                {
                    "items": [
                        {
                            "itemId": %d,
                            "quantity": 2
                        },
                        {
                            "itemId": %d,
                            "quantity": 3
                        }
                    ]
                }
                """.formatted(
                item.getId(),
                secondItem.getId()
        );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(user(currentUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price")
                        .value(2150.00));

        List<Order> orders =
                orderRepository.findAll();

        assertThat(orders)
                .hasSize(1);

        assertThat(orders.get(0).getPrice())
                .isEqualByComparingTo("2150.00");

        List<OrderItem> orderItems =
                orderItemRepository.findAll();

        assertThat(orderItems)
                .hasSize(2);

        assertThat(orderItems)
                .extracting(OrderItem::getQuantity)
                .containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void createOrder_shouldRejectWhenItemDoesNotExist() throws Exception {

        long nonexistentItemId = 999999L;

        String json = """
            {
                "items": [
                    {
                        "itemId": %d,
                        "quantity": 1
                    }
                ]
            }
            """.formatted(nonexistentItemId);

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(user(currentUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound());

        assertThat(orderRepository.findAll())
                .isEmpty();
        assertThat(orderItemRepository.findAll())
                .isEmpty();
    }

    @Test
    void createOrder_shouldRejectInactiveItem() throws Exception {

        item.setStatus(ItemStatus.INACTIVE);
        itemRepository.saveAndFlush(item);

        String json = """
            {
                "items": [
                    {
                        "itemId": %d,
                        "quantity": 1
                    }
                ]
            }
            """.formatted(item.getId());

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(user(currentUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.findAll())
                .isEmpty();
        assertThat(orderItemRepository.findAll())
                .isEmpty();
    }

    @Test
    void createOrder_shouldRejectInvalidQuantity() throws Exception {

        String json = """
                {
                    "items": [
                        {
                            "itemId": %d,
                            "quantity": 0
                        }
                    ]
                }
                """.formatted(item.getId());

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(user(currentUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldRejectNullItems() throws Exception {

        String json = """
                {
                    "items": null
                }
                """;

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(user(currentUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_shouldReturnOwnOrder() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        false
                );

        mockMvc.perform(
                        get("/api/v1/orders/{id}",
                                order.getOrderPublicId())
                                .with(user(currentUserId))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orderPublicId")
                                .value(
                                        order.getOrderPublicId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CREATED")
                );
    }

    @Test
    void getOrder_shouldRejectAnotherUser() throws Exception {

        Order order =
                createOrder(
                        anotherUserId,
                        OrderStatus.CREATED,
                        false
                );

        mockMvc.perform(
                        get("/api/v1/orders/{id}",
                                order.getOrderPublicId())
                                .with(user(currentUserId))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrder_shouldAllowAdminToViewAnotherUserOrder()
            throws Exception {

        Order order =
                createOrder(
                        anotherUserId,
                        OrderStatus.CREATED,
                        false
                );

        when(userInfoProvider.getUserInfo(anotherUserId))
                .thenReturn(
                        org.example.order_service.dto.UserInfo.builder()
                                .publicId(anotherUserId)
                                .name("Jane")
                                .surname("Doe")
                                .email("jane@test.com")
                                .build()
                );

        mockMvc.perform(
                        get("/api/v1/orders/{id}",
                                order.getOrderPublicId())
                                .with(admin())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.userPublicId")
                                .value(
                                        anotherUserId.toString()
                                )
                );
    }

    @Test
    void getOrder_shouldRejectDeletedOrder() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        true
                );

        mockMvc.perform(
                        get("/api/v1/orders/{id}",
                                order.getOrderPublicId())
                                .with(user(currentUserId))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_shouldReturnNotFound_whenOrderDoesNotExist()
            throws Exception {

        UUID randomOrderId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/orders/{id}", randomOrderId)
                                .with(user(currentUserId))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrders_shouldReturnOnlyCurrentUserOrders() throws Exception {

        createOrder(
                currentUserId,
                OrderStatus.CREATED,
                false
        );

        createOrder(
                currentUserId,
                OrderStatus.COMPLETED,
                false
        );

        createOrder(
                anotherUserId,
                OrderStatus.CREATED,
                false
        );

        mockMvc.perform(
                        get("/api/v1/orders")
                                .with(user(currentUserId))
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(2)
                );
    }

    @Test
    void getOrders_shouldFilterByStatus() throws Exception {

        createOrder(
                currentUserId,
                OrderStatus.CREATED,
                false
        );

        createOrder(
                currentUserId,
                OrderStatus.COMPLETED,
                false
        );

        mockMvc.perform(
                        get("/api/v1/orders")
                                .with(user(currentUserId))
                                .param(
                                        "status",
                                        "COMPLETED"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].status")
                                .value("COMPLETED")
                );
    }

    @Test
    void getOrders_shouldRejectAnotherUser() throws Exception {

        mockMvc.perform(
                        get("/api/v1/orders")
                                .with(user(currentUserId))
                                .param(
                                        "userPublicId",
                                        anotherUserId.toString()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrders_shouldAcceptDateRangeParameters() throws Exception {

        mockMvc.perform(
                        get("/api/v1/orders")
                                .with(user(currentUserId))
                                .param(
                                        "fromDate",
                                        "2026-01-01T00:00:00"
                                )
                                .param(
                                        "toDate",
                                        "2026-12-31T23:59:59"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void updateOrder_shouldRequireAdmin() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        false
                );

        String json = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/status",
                                order.getOrderPublicId()
                        )
                                .with(user(currentUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOrder_shouldUpdateForAdmin() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        false
                );

        String json = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/status",
                                order.getOrderPublicId()
                        )
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );

        Order updated =
                orderRepository
                        .findByOrderPublicId(
                                order.getOrderPublicId()
                        )
                        .orElseThrow();

        assertThat(updated.getStatus())
                .isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void updateOrder_shouldRejectDeletedOrder()
            throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        true
                );

        String json = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/status",
                                order.getOrderPublicId()
                        )
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrder_shouldReturnNotFound_whenOrderDoesNotExist()
            throws Exception {

        UUID randomOrderId = UUID.randomUUID();

        String json = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/status",
                                randomOrderId
                        )
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrder_shouldRequireAdmin() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        false
                );

        mockMvc.perform(
                        delete(
                                "/api/v1/orders/{id}",
                                order.getOrderPublicId()
                        )
                                .with(user(currentUserId))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOrder_shouldSoftDeleteForAdmin() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        false
                );

        mockMvc.perform(
                        delete(
                                "/api/v1/orders/{id}",
                                order.getOrderPublicId()
                        )
                                .with(admin())
                )
                .andExpect(status().isNoContent());

        Order deleted =
                orderRepository
                        .findByOrderPublicId(
                                order.getOrderPublicId()
                        )
                        .orElseThrow();

        assertThat(deleted.getDeleted())
                .isTrue();
    }

    @Test
    void deleteOrder_shouldReturnNotFound_whenOrderDoesNotExist()
            throws Exception {

        UUID randomOrderId = UUID.randomUUID();

        mockMvc.perform(
                        delete(
                                "/api/v1/orders/{id}",
                                randomOrderId
                        )
                                .with(admin())
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void reviveOrder_shouldRequireAdmin() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        true
                );

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/revive",
                                order.getOrderPublicId()
                        )
                                .with(user(currentUserId))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void reviveOrder_shouldRestoreDeletedOrder() throws Exception {

        Order order =
                createOrder(
                        currentUserId,
                        OrderStatus.CREATED,
                        true
                );

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/revive",
                                order.getOrderPublicId()
                        )
                                .with(admin())
                )
                .andExpect(status().isNoContent());

        Order revived =
                orderRepository
                        .findByOrderPublicId(
                                order.getOrderPublicId()
                        )
                        .orElseThrow();

        assertThat(revived.getDeleted())
                .isFalse();
    }

    @Test
    void reviveOrder_shouldReturnNotFound_whenOrderDoesNotExist()
            throws Exception {

        UUID randomOrderId = UUID.randomUUID();

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/revive",
                                randomOrderId
                        )
                                .with(admin())
                )
                .andExpect(status().isNotFound());
    }

    private Order createOrder(
            UUID userId,
            OrderStatus status,
            boolean deleted) {

        Order order = Order.builder()
                .orderPublicId(UUID.randomUUID())
                .userPublicId(userId)
                .status(status)
                .price(new BigDecimal("100.00"))
                .deleted(deleted)
                .build();

        return orderRepository.saveAndFlush(order);
    }

    private RequestPostProcessor user(UUID userId) {

        return authentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_USER"
                                )
                        )
                )
        );
    }

    private RequestPostProcessor admin() {

        return authentication(
                new UsernamePasswordAuthenticationToken(
                        currentUserId.toString(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMIN"
                                )
                        )
                )
        );
    }
}