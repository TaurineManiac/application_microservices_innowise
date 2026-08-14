package org.example.order_service.unit;

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
import org.example.order_service.service.OrderItemService;
import org.example.order_service.service.OrderService;
import org.example.order_service.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private UserInfoProvider userInfoProvider;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private UUID currentUserId;
    private UUID anotherUserId;
    private UUID orderPublicId;

    private Item item;
    private Order order;
    private OrderItem orderItem;
    private OrderResponse response;
    private UserInfo userInfo;


    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {

        currentUserId = UUID.randomUUID();
        anotherUserId = UUID.randomUUID();
        orderPublicId = UUID.randomUUID();


        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserPublicId).thenReturn(currentUserId);
        securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);

        item = Item.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("1200.00"))
                .status(ItemStatus.ACTIVE)
                .build();

        order = Order.builder()
                .id(1L)
                .orderPublicId(orderPublicId)
                .userPublicId(currentUserId)
                .status(OrderStatus.CREATED)
                .price(BigDecimal.ZERO)
                .deleted(false)
                .build();

        orderItem = OrderItem.builder()
                .id(1L)
                .order(order)
                .item(item)
                .quantity(2)
                .itemsPriceAtMomentOfOrder(new BigDecimal("1200.00"))
                .build();

        response = OrderResponse.builder()
                .id(1L)
                .orderPublicId(orderPublicId)
                .userPublicId(currentUserId)
                .status(OrderStatus.CREATED)
                .price(BigDecimal.ZERO)
                .deleted(false)
                .build();

        userInfo = UserInfo.builder()
                .publicId(currentUserId)
                .name("John")
                .surname("Doe")
                .email("john@test.com")
                .build();
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }


    private void mockSecurityAdmin() {
        securityUtilsMock
                .when(SecurityUtils::getCurrentUserPublicId)
                .thenReturn(currentUserId);

        securityUtilsMock
                .when(SecurityUtils::isAdmin)
                .thenReturn(true);
    }


    @Test
    void createOrder_shouldCreateOrder() {

        CreateOrderRequest request =
                CreateOrderRequest.builder()
                        .items(List.of(
                                CreateOrderItemRequest.builder()
                                        .itemId(1L)
                                        .quantity(2)
                                        .build()
                        ))
                        .build();

        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(item));

        when(orderItemService.createOrderItem(
                eq(item),
                eq(2),
                any(Order.class)
        )).thenReturn(orderItem);

        when(orderItemService.calculateTotalPrice(item, 2))
                .thenReturn(new BigDecimal("2400.00"));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        OrderResponse result =
                orderService.createOrder(request);

        assertThat(result)
                .isEqualTo(response);

        assertThat(result.getUserInfo())
                .isEqualTo(userInfo);

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository)
                .save(captor.capture());

        Order savedOrder = captor.getValue();

        assertThat(savedOrder.getOrderPublicId())
                .isNotNull();

        assertThat(savedOrder.getUserPublicId())
                .isEqualTo(currentUserId);

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(savedOrder.getPrice())
                .isEqualByComparingTo("2400.00");

        assertThat(savedOrder.getDeleted())
                .isFalse();

        assertThat(savedOrder.getOrderItems())
                .containsExactly(orderItem);
    }

    @Test
    void createOrder_shouldCalculateTotalForMultipleItems() {

        Item secondItem = Item.builder()
                .id(2L)
                .name("Mouse")
                .price(new BigDecimal("50.00"))
                .status(ItemStatus.ACTIVE)
                .build();

        OrderItem secondOrderItem =
                OrderItem.builder()
                        .id(2L)
                        .order(order)
                        .item(secondItem)
                        .quantity(3)
                        .itemsPriceAtMomentOfOrder(
                                new BigDecimal("50.00")
                        )
                        .build();

        CreateOrderRequest request =
                CreateOrderRequest.builder()
                        .items(List.of(
                                CreateOrderItemRequest.builder()
                                        .itemId(1L)
                                        .quantity(2)
                                        .build(),
                                CreateOrderItemRequest.builder()
                                        .itemId(2L)
                                        .quantity(3)
                                        .build()
                        ))
                        .build();

        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(item));

        when(itemRepository.findById(2L))
                .thenReturn(Optional.of(secondItem));

        when(orderItemService.createOrderItem(
                eq(item),
                eq(2),
                any(Order.class)
        )).thenReturn(orderItem);

        when(orderItemService.createOrderItem(
                eq(secondItem),
                eq(3),
                any(Order.class)
        )).thenReturn(secondOrderItem);

        when(orderItemService.calculateTotalPrice(item, 2))
                .thenReturn(new BigDecimal("2400.00"));

        when(orderItemService.calculateTotalPrice(secondItem, 3))
                .thenReturn(new BigDecimal("150.00"));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        orderService.createOrder(request);

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository)
                .save(captor.capture());

        Order savedOrder = captor.getValue();

        assertThat(savedOrder.getPrice())
                .isEqualByComparingTo("2550.00");

        assertThat(savedOrder.getOrderItems())
                .containsExactlyInAnyOrder(
                        orderItem,
                        secondOrderItem
                );
    }

    @Test
    void createOrder_shouldThrowNotFound_whenItemDoesNotExist() {

        CreateOrderRequest request =
                CreateOrderRequest.builder()
                        .items(List.of(
                                CreateOrderItemRequest.builder()
                                        .itemId(999L)
                                        .quantity(1)
                                        .build()
                        ))
                        .build();

        when(itemRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> orderService.createOrder(request)
        )
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Item not found");

        verify(orderRepository, never())
                .save(any(Order.class));

        verify(orderItemService, never())
                .createOrderItem(
                        any(Item.class),
                        anyInt(),
                        any(Order.class)
                );
    }

    @Test
    void createOrder_shouldThrowException_whenItemInactive() {

        item.setStatus(ItemStatus.INACTIVE);

        CreateOrderRequest request =
                CreateOrderRequest.builder()
                        .items(List.of(
                                CreateOrderItemRequest.builder()
                                        .itemId(1L)
                                        .quantity(1)
                                        .build()
                        ))
                        .build();

        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(
                () -> orderService.createOrder(request)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Item is not active");

        verify(orderRepository, never())
                .save(any(Order.class));

        verify(orderItemService, never())
                .createOrderItem(
                        any(Item.class),
                        anyInt(),
                        any(Order.class)
                );
    }

    @Test
    void getOrderById_shouldReturnOrder() {

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        OrderResponse result =
                orderService.getOrderById(orderPublicId);

        assertThat(result)
                .isEqualTo(response);

        assertThat(result.getUserInfo())
                .isEqualTo(userInfo);

        verify(orderRepository)
                .findByOrderPublicId(orderPublicId);

        verify(orderMapper)
                .toResponse(order);

        verify(userInfoProvider)
                .getUserInfo(currentUserId);
    }

    @Test
    void getOrderById_shouldThrowNotFound() {

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> orderService.getOrderById(orderPublicId)
        )
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderMapper, never())
                .toResponse(any(Order.class));
    }

    @Test
    void getOrderById_shouldThrowException_whenDeleted() {

        order.setDeleted(true);

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(
                () -> orderService.getOrderById(orderPublicId)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order is already deleted");

        verify(orderMapper, never())
                .toResponse(any(Order.class));
    }

    @Test
    void getOrderById_shouldDenyAnotherUser() {

        order.setUserPublicId(anotherUserId);

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(
                () -> orderService.getOrderById(orderPublicId)
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(orderMapper, never())
                .toResponse(any(Order.class));
    }

    @Test
    void getOrderById_shouldAllowAdmin() {

        mockSecurityAdmin();

        order.setUserPublicId(anotherUserId);
        response.setUserPublicId(anotherUserId);

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        UserInfo anotherUserInfo = UserInfo.builder()
                .publicId(anotherUserId)
                .name("John")
                .surname("Doe")
                .email("john@test.com")
                .build();

        when(userInfoProvider.getUserInfo(anotherUserId))
                .thenReturn(anotherUserInfo);

        OrderResponse result =
                orderService.getOrderById(orderPublicId);

        assertThat(result)
                .isEqualTo(response);

        verify(orderMapper)
                .toResponse(order);

        verify(userInfoProvider)
                .getUserInfo(anotherUserId);
    }


    @Test
    void getOrders_shouldReturnCurrentUserOrders() {

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Order> page =
                new PageImpl<>(List.of(order));

        when(orderRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(page);

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        Page<OrderResponse> result =
                orderService.getOrders(
                        null,
                        null,
                        null,
                        null,
                        pageable
                );

        assertThat(result.getContent())
                .containsExactly(response);

        verify(orderRepository)
                .findAll(
                        any(Specification.class),
                        eq(pageable)
                );
    }

    @Test
    void getOrders_shouldRejectAnotherUserForRegularUser() {

        Pageable pageable =
                PageRequest.of(0, 10);

        assertThatThrownBy(
                () -> orderService.getOrders(
                        anotherUserId,
                        null,
                        null,
                        null,
                        pageable
                )
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(
                        "only view your own orders"
                );

        verify(orderRepository, never())
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }

    @Test
    void getOrders_shouldAllowAdminToRequestAnotherUser() {

        mockSecurityAdmin();

        order.setUserPublicId(anotherUserId);
        response.setUserPublicId(anotherUserId);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> page =
                new PageImpl<>(List.of(order));

        when(orderRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(page);

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        UserInfo anotherUserInfo = UserInfo.builder()
                .publicId(anotherUserId)
                .name("John")
                .surname("Doe")
                .email("john@test.com")
                .build();

        when(userInfoProvider.getUserInfo(anotherUserId))
                .thenReturn(anotherUserInfo);

        Page<OrderResponse> result =
                orderService.getOrders(
                        anotherUserId,
                        null,
                        null,
                        null,
                        pageable
                );

        assertThat(result.getContent())
                .containsExactly(response);

        verify(orderRepository)
                .findAll(
                        any(Specification.class),
                        eq(pageable)
                );

        verify(userInfoProvider)
                .getUserInfo(anotherUserId);
    }
    @Test
    void getOrders_shouldPassFiltersToRepository() {

        Pageable pageable =
                PageRequest.of(0, 10);

        LocalDateTime from =
                LocalDateTime.of(2026, 1, 1, 0, 0);

        LocalDateTime to =
                LocalDateTime.of(2026, 12, 31, 23, 59);

        Page<Order> page =
                new PageImpl<>(List.of(order));

        when(orderRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(page);

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        Page<OrderResponse> result =
                orderService.getOrders(
                        currentUserId,
                        OrderStatus.CREATED,
                        from,
                        to,
                        pageable
                );

        assertThat(result.getContent())
                .containsExactly(response);

        verify(orderRepository)
                .findAll(
                        any(Specification.class),
                        eq(pageable)
                );
    }

    @Test
    void getOrdersByUser_shouldDelegateToGetOrders() {

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Order> page =
                new PageImpl<>(List.of(order));

        when(orderRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(page);

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        Page<OrderResponse> result =
                orderService.getOrdersByUser(
                        currentUserId,
                        pageable
                );

        assertThat(result.getContent())
                .containsExactly(response);
    }



    @Test
    void updateOrder_shouldUpdateStatus() {

        UpdateOrderStatusRequest request =
                UpdateOrderStatusRequest.builder()
                        .status(OrderStatus.COMPLETED)
                        .build();

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        OrderResponse result =
                orderService.updateOrder(
                        orderPublicId,
                        request
                );

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.COMPLETED);

        assertThat(result)
                .isEqualTo(response);

        verify(orderRepository)
                .save(order);
    }

    @Test
    void updateOrder_shouldNotChangeStatus_whenStatusNull() {

        UpdateOrderStatusRequest request =
                UpdateOrderStatusRequest.builder()
                        .status(null)
                        .build();

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        when(orderMapper.toResponse(order))
                .thenReturn(response);

        when(userInfoProvider.getUserInfo(currentUserId))
                .thenReturn(userInfo);

        orderService.updateOrder(
                orderPublicId,
                request
        );

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        verify(orderRepository)
                .save(order);
    }

    @Test
    void updateOrder_shouldRejectDeletedOrder() {

        order.setDeleted(true);

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request =
                UpdateOrderStatusRequest.builder()
                        .status(OrderStatus.COMPLETED)
                        .build();

        assertThatThrownBy(
                () -> orderService.updateOrder(
                        orderPublicId,
                        request
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order is already deleted");

        verify(orderRepository, never())
                .save(any(Order.class));
    }



    @Test
    void deleteOrder_shouldSoftDelete() {

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        orderService.deleteOrder(orderPublicId);

        assertThat(order.getDeleted())
                .isTrue();

        verify(orderRepository)
                .save(order);
    }

    @Test
    void deleteOrder_shouldRejectAnotherUser() {

        order.setUserPublicId(anotherUserId);

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(
                () -> orderService.deleteOrder(orderPublicId)
        )
                .isInstanceOf(AccessDeniedException.class);

        verify(orderRepository, never())
                .save(any(Order.class));
    }


    @Test
    void reviveOrder_shouldReviveDeletedOrderForOwner() {

        order.setDeleted(true);

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        orderService.reviveOrder(orderPublicId);

        assertThat(order.getDeleted())
                .isFalse();

        verify(orderRepository)
                .save(order);
    }

    @Test
    void reviveOrder_shouldAllowAdmin() {

        order.setUserPublicId(anotherUserId);
        order.setDeleted(true);

        mockSecurityAdmin();

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        orderService.reviveOrder(orderPublicId);

        assertThat(order.getDeleted())
                .isFalse();

        verify(orderRepository)
                .save(order);
    }

    @Test
    void reviveOrder_shouldThrowNotFound() {

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> orderService.reviveOrder(orderPublicId)
        )
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void reviveOrder_shouldRejectAnotherUser() {

        order.setUserPublicId(anotherUserId);
        order.setDeleted(true);

        when(orderRepository.findByOrderPublicId(orderPublicId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(
                () -> orderService.reviveOrder(orderPublicId)
        )
                .isInstanceOf(AccessDeniedException.class);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
}