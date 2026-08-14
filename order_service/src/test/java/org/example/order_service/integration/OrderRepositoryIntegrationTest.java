package org.example.order_service.integration;

import org.example.order_service.entity.Order;
import org.example.order_service.enums.OrderStatus;
import org.example.order_service.repository.OrderRepository;
import org.example.order_service.specification.OrderSpecification;
import org.example.order_service.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;


    private UUID userId;
    private UUID anotherUserId;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Test
    void shouldFilterByDateRange() {

        Order oldOrder = orderRepository.saveAndFlush(createOrder(UUID.randomUUID(), userId, OrderStatus.CREATED, new BigDecimal("100.00"), false, null));
        Order targetOrder = orderRepository.saveAndFlush(createOrder(UUID.randomUUID(), userId, OrderStatus.CREATED, new BigDecimal("200.00"), false, null));
        Order futureOrder = orderRepository.saveAndFlush(createOrder(UUID.randomUUID(), userId, OrderStatus.CREATED, new BigDecimal("300.00"), false, null));


        updateDate(oldOrder.getId(), LocalDateTime.of(2025, 1, 1, 12, 0));
        updateDate(targetOrder.getId(), LocalDateTime.of(2026, 6, 1, 12, 0));
        updateDate(futureOrder.getId(), LocalDateTime.of(2027, 1, 1, 12, 0));


        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);

        Specification<Order> specification = Specification
                .where(OrderSpecification.notDeleted())
                .and(OrderSpecification.hasUserPublicId(userId))
                .and(OrderSpecification.createdBetween(from, to));

        Page<Order> result = orderRepository.findAll(specification, PageRequest.of(0, 10));


        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPrice()).isEqualByComparingTo("200.00");
    }


    private void updateDate(Long id, LocalDateTime dateTime) {

        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(dateTime), id);
    }

    @BeforeEach
    void setUp() {

        orderRepository.deleteAll();

        userId = UUID.randomUUID();
        anotherUserId = UUID.randomUUID();
    }

    @Test
    void shouldSaveAndFindOrderByPublicId() {

        UUID orderPublicId = UUID.randomUUID();

        Order order = createOrder(
                orderPublicId,
                userId,
                OrderStatus.CREATED,
                new BigDecimal("100.00"),
                false,
                LocalDateTime.now()
        );

        orderRepository.saveAndFlush(order);

        Order found =
                orderRepository
                        .findByOrderPublicId(orderPublicId)
                        .orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getOrderPublicId())
                .isEqualTo(orderPublicId);
        assertThat(found.getUserPublicId())
                .isEqualTo(userId);
        assertThat(found.getStatus())
                .isEqualTo(OrderStatus.CREATED);
        assertThat(found.getPrice())
                .isEqualByComparingTo("100.00");
        assertThat(found.getDeleted())
                .isFalse();
    }

    @Test
    void shouldFindOrdersByUserUsingSpecification() {

        Order first = createOrder(
                UUID.randomUUID(),
                userId,
                OrderStatus.CREATED,
                new BigDecimal("100.00"),
                false,
                LocalDateTime.of(2026, 1, 10, 10, 0)
        );

        Order second = createOrder(
                UUID.randomUUID(),
                userId,
                OrderStatus.COMPLETED,
                new BigDecimal("200.00"),
                false,
                LocalDateTime.of(2026, 2, 10, 10, 0)
        );

        Order anotherUserOrder = createOrder(
                UUID.randomUUID(),
                anotherUserId,
                OrderStatus.CREATED,
                new BigDecimal("300.00"),
                false,
                LocalDateTime.of(2026, 2, 10, 10, 0)
        );

        orderRepository.saveAll(
                List.of(first, second, anotherUserOrder)
        );

        Pageable pageable = PageRequest.of(0, 10);

        Specification<Order> specification =
                Specification
                        .where(OrderSpecification.notDeleted())
                        .and(OrderSpecification.hasUserPublicId(userId));

        Page<Order> result =
                orderRepository.findAll(
                        specification,
                        pageable
                );

        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getContent())
                .extracting(Order::getUserPublicId)
                .containsOnly(userId);
    }

    @Test
    void shouldExcludeDeletedOrders() {

        Order activeOrder = createOrder(
                UUID.randomUUID(),
                userId,
                OrderStatus.CREATED,
                new BigDecimal("100.00"),
                false,
                LocalDateTime.now()
        );

        Order deletedOrder = createOrder(
                UUID.randomUUID(),
                userId,
                OrderStatus.CREATED,
                new BigDecimal("200.00"),
                true,
                LocalDateTime.now()
        );

        orderRepository.saveAll(
                List.of(activeOrder, deletedOrder)
        );

        Specification<Order> specification =
                Specification
                        .where(OrderSpecification.notDeleted())
                        .and(OrderSpecification.hasUserPublicId(userId));

        Page<Order> result =
                orderRepository.findAll(
                        specification,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).getDeleted())
                .isFalse();
    }

    @Test
    void shouldFilterByStatus() {

        Order created = createOrder(
                UUID.randomUUID(),
                userId,
                OrderStatus.CREATED,
                new BigDecimal("100.00"),
                false,
                LocalDateTime.now()
        );

        Order completed = createOrder(
                UUID.randomUUID(),
                userId,
                OrderStatus.COMPLETED,
                new BigDecimal("200.00"),
                false,
                LocalDateTime.now()
        );

        orderRepository.saveAll(
                List.of(created, completed)
        );

        Specification<Order> specification =
                Specification
                        .where(OrderSpecification.notDeleted())
                        .and(OrderSpecification.hasUserPublicId(userId))
                        .and(OrderSpecification.hasStatus(
                                OrderStatus.COMPLETED
                        ));

        Page<Order> result =
                orderRepository.findAll(
                        specification,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).getStatus())
                .isEqualTo(OrderStatus.COMPLETED);
    }



    private Order createOrder(
            UUID orderPublicId,
            UUID userPublicId,
            OrderStatus status,
            BigDecimal price,
            boolean deleted,
            LocalDateTime createdAt) {

        return Order.builder()
                .orderPublicId(orderPublicId)
                .userPublicId(userPublicId)
                .status(status)
                .price(price)
                .deleted(deleted)
                .createdAt(createdAt)
                .build();
    }
}