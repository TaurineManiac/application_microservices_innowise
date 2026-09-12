package org.example.payment_service.integration;

import org.example.payment_service.entity.Payment;
import org.example.payment_service.enums.PaymentStatus;
import org.example.payment_service.repository.PaymentRepository;
import org.example.payment_service.specification.PaymentSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentRepository repository;

    private UUID user1;
    private UUID user2;
    private UUID order1;
    private UUID order2;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
        order1 = UUID.randomUUID();
        order2 = UUID.randomUUID();

        Payment p1 = Payment.builder()
                .userPublicId(user1)
                .orderPublicId(order1)
                .amount(new BigDecimal("100.50"))
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        Payment p2 = Payment.builder()
                .userPublicId(user1)
                .orderPublicId(order2)
                .amount(new BigDecimal("200.00"))
                .status(PaymentStatus.FAILED)
                .createdAt(LocalDateTime.now().minusHours(5))
                .updatedAt(LocalDateTime.now().minusHours(5))
                .build();

        Payment p3 = Payment.builder()
                .userPublicId(user2)
                .orderPublicId(order1)
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(p1, p2, p3));
    }

    @Test
    void shouldFindAllWithUserFilter() {
        Specification<Payment> spec = PaymentSpecification.hasUserPublicId(user1);
        Page<Payment> page = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void shouldFilterByStatus() {
        Specification<Payment> spec = PaymentSpecification.hasStatus(PaymentStatus.COMPLETED);
        Page<Payment> page = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void shouldFilterByOrderId() {
        Specification<Payment> spec = PaymentSpecification.hasOrderPublicId(order1);
        Page<Payment> page = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void shouldCombineFilters() {
        Specification<Payment> spec = Specification
                .where(PaymentSpecification.hasUserPublicId(user1))
                .and(PaymentSpecification.hasStatus(PaymentStatus.COMPLETED));
        Page<Payment> page = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void shouldFilterByAmountRange() {
        Specification<Payment> spec = PaymentSpecification.amountBetween(new BigDecimal("50"), new BigDecimal("150"));
        Page<Payment> page = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2); // p1 и p3
    }

    @Test
    void shouldReturnEmptyForNonMatchingFilters() {
        Specification<Payment> spec = PaymentSpecification.hasUserPublicId(UUID.randomUUID());
        Page<Payment> page = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void shouldCalculateTotalForUser() {
        BigDecimal total = repository.getTotalAmountForUser(user1,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(1));
        assertThat(total).isEqualByComparingTo(new BigDecimal("300.50"));
    }

    @Test
    void shouldCalculateTotalForAllUsers() {
        BigDecimal total = repository.getTotalAmount(
                LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(1));
        assertThat(total).isEqualByComparingTo(new BigDecimal("350.50"));
    }
}