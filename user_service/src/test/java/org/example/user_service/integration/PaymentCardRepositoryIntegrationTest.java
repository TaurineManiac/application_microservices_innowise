package org.example.user_service.integration;

import org.example.user_service.entity.PaymentCard;
import org.example.user_service.entity.User;
import org.example.user_service.repository.PaymentCardRepository;
import org.example.user_service.repository.UserRepository;
import org.example.user_service.specification.PaymentCardSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Testcontainers
public class PaymentCardRepositoryIntegrationTest {

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
        registry.add("internal.service.token", () -> "test-token");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        paymentCardRepository.deleteAll();

        user = User.builder()
                .publicId(UUID.randomUUID())
                .name("Card")
                .surname("Holder")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("cardholder@test.com")
                .active(true)
                .build();
        userRepository.save(user);
    }

    @Test
    void shouldCountCardsByUserId() {
        PaymentCard card1 = PaymentCard.builder()
                .number("1111111111111111")
                .holder("Card Holder")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(true)
                .user(user)
                .build();

        PaymentCard card2 = PaymentCard.builder()
                .number("2222222222222222")
                .holder("Card Holder")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(false)
                .user(user)
                .build();

        paymentCardRepository.saveAll(List.of(card1, card2));

        long count = paymentCardRepository.countByUserId(user.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindAllByUserId() {
        PaymentCard card = PaymentCard.builder()
                .number("3333333333333333")
                .holder("Card Holder")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(true)
                .user(user)
                .build();

        paymentCardRepository.save(card);

        List<PaymentCard> cards = paymentCardRepository.findAllByUser_Id(user.getId());
        assertThat(cards).hasSize(1);
    }

    @Test
    void shouldFindBySpecification() {
        PaymentCard card1 = PaymentCard.builder()
                .number("4444444444444444")
                .holder("John")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(true)
                .user(user)
                .build();

        PaymentCard card2 = PaymentCard.builder()
                .number("5555555555555555")
                .holder("Jane")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(false)
                .user(user)
                .build();

        paymentCardRepository.saveAll(List.of(card1, card2));

        Specification<PaymentCard> spec = Specification
                .where(PaymentCardSpecification.isActive(true));

        List<PaymentCard> activeCards = paymentCardRepository.findAll(spec);
        assertThat(activeCards).hasSize(1);
        assertThat(activeCards.get(0).getHolder()).isEqualTo("John");
    }
}