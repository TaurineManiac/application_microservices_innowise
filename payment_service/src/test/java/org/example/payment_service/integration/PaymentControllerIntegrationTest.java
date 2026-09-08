package org.example.payment_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.payment_service.dto.PaymentRequestEvent;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.enums.PaymentStatus;
import org.example.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
@WithMockUser(roles = "USER")
class PaymentControllerIntegrationTest {

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

    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;
    private UUID userPublicId;
    private UUID orderPublicId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userPublicId = UUID.randomUUID();
        orderPublicId = UUID.randomUUID();

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

    @Test
    void shouldGetPaymentsForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
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
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
    @WithMockUser(roles = "USER")
    void shouldFilterByPaymentStatus() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .param("paymentStatus", "COMPLETED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldFilterByOrderId() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .param("orderPublicId", orderPublicId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnEmptyPageWhenNoMatches() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .param("userPublicId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldGetTotalForUser() throws Exception {
        mockMvc.perform(get("/api/v1/payments/total/user")
                        .param("userPublicId", userPublicId.toString())
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2030-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetTotalForAllUsersAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/payments/total/all")
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2030-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidUserFromGettingTotalAll() throws Exception {
        mockMvc.perform(get("/api/v1/payments/total/all")
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2030-01-01T00:00:00"))
                .andExpect(status().isForbidden());
    }
}