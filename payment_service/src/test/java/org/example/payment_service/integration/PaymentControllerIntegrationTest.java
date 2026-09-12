package org.example.payment_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.payment_service.dto.PaymentRequestEvent;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.enums.PaymentStatus;
import org.example.payment_service.kafka.PaymentEventProducer;
import org.example.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
class PaymentControllerIntegrationTest {

    private static final UUID TEST_USER_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_ORDER_UUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");

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
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("internal.service.token", () -> "test-token");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentEventProducer eventProducer;

    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;
    private UUID userPublicId;
    private UUID orderPublicId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        userPublicId = TEST_USER_UUID;
        orderPublicId = TEST_ORDER_UUID;

        Payment payment = Payment.builder()
                .userPublicId(userPublicId)
                .orderPublicId(orderPublicId)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);
    }

    private RequestPostProcessor asUser(UUID userId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private RequestPostProcessor asAdmin() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void shouldGetPaymentsForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .with(asUser(userPublicId))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateInternalPaymentWithValidToken() throws Exception {
        PaymentRequestEvent request = PaymentRequestEvent.builder()
                .orderPublicId(UUID.randomUUID())
                .userPublicId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments/internal")
                        .header("X-Internal-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    void shouldFilterByPaymentStatus() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .with(asUser(userPublicId))
                        .param("paymentStatus", "COMPLETED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterByOrderId() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .with(asUser(userPublicId))
                        .param("orderPublicId", orderPublicId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldForbidFilteringByAnotherUsersId() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .with(asUser(userPublicId))
                        .param("userPublicId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetTotalForUser() throws Exception {
        mockMvc.perform(get("/api/v1/payments/total/user")
                        .with(asUser(userPublicId))
                        .param("userPublicId", userPublicId.toString())
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2030-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100.00));
    }

    @Test
    void shouldGetTotalForAllUsersAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/payments/total/all")
                        .with(asAdmin())
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2030-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100.00));
    }

    @Test
    void shouldForbidUserFromGettingTotalAll() throws Exception {
        mockMvc.perform(get("/api/v1/payments/total/all")
                        .with(asUser(userPublicId))
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2030-01-01T00:00:00"))
                .andExpect(status().isForbidden());
    }
}