package org.example.user_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redis.testcontainers.RedisContainer;
import org.example.user_service.dto.CreatePaymentCardRequest;
import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.PaymentCardResponse;
import org.example.user_service.dto.UpdateCardRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
class PaymentCardControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private UUID userPublicId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .name("Card")
                .surname("Owner")
                .email("card@test.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        String userJson = objectMapper.writeValueAsString(userRequest);
        String userResponse = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse createdUser = objectMapper.readValue(userResponse, UserResponse.class);
        userPublicId = createdUser.getPublicId();
    }

    @Test
    void createCard_shouldReturnCreated() throws Exception {
        CreatePaymentCardRequest cardRequest = CreatePaymentCardRequest.builder()
                .holder("Card Owner")
                .build();

        mockMvc.perform(post("/api/v1/users/{publicUserId}/cards", userPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.holder").value("Card Owner"))
                .andExpect(jsonPath("$.number").exists())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getCardsByUser_shouldReturnPage() throws Exception {
        CreatePaymentCardRequest cardRequest = CreatePaymentCardRequest.builder()
                .holder("Card Owner")
                .build();

        mockMvc.perform(post("/api/v1/users/{publicUserId}/cards", userPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users/{publicUserId}/cards", userPublicId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].holder").value("Card Owner"));
    }

    @Test
    void getCardById_shouldReturnCard() throws Exception {
        CreatePaymentCardRequest cardRequest = CreatePaymentCardRequest.builder()
                .holder("Card Owner")
                .build();

        String response = mockMvc.perform(post("/api/v1/users/{publicUserId}/cards", userPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        PaymentCardResponse createdCard = objectMapper.readValue(response, PaymentCardResponse.class);
        Long cardId = createdCard.getId();

        mockMvc.perform(get("/api/v1/users/{publicUserId}/cards/{cardId}", userPublicId, cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.holder").value("Card Owner"));
    }

    @Test
    void updateCard_shouldUpdateAndReturnOk() throws Exception {
        CreatePaymentCardRequest cardRequest = CreatePaymentCardRequest.builder()
                .holder("Old Holder")
                .build();

        String response = mockMvc.perform(post("/api/v1/users/{publicUserId}/cards", userPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        PaymentCardResponse createdCard = objectMapper.readValue(response, PaymentCardResponse.class);
        Long cardId = createdCard.getId();

        UpdateCardRequest updateRequest = UpdateCardRequest.builder()
                .holder("New Holder")
                .build();

        mockMvc.perform(put("/api/v1/users/{publicUserId}/cards/{cardId}", userPublicId, cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("New Holder"));
    }

    @Test
    void activateCard_shouldReturnNoContent() throws Exception {
        CreatePaymentCardRequest cardRequest = CreatePaymentCardRequest.builder()
                .holder("Card Owner")
                .build();

        String response = mockMvc.perform(post("/api/v1/users/{publicUserId}/cards", userPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        PaymentCardResponse createdCard = objectMapper.readValue(response, PaymentCardResponse.class);
        Long cardId = createdCard.getId();

        mockMvc.perform(patch("/api/v1/users/{publicUserId}/cards/{cardId}/deactivate", userPublicId, cardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/users/{publicUserId}/cards/{cardId}/activate", userPublicId, cardId))
                .andExpect(status().isNoContent());

        String getResponse = mockMvc.perform(get("/api/v1/users/{publicUserId}/cards/{cardId}", userPublicId, cardId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        PaymentCardResponse card = objectMapper.readValue(getResponse, PaymentCardResponse.class);
        assertThat(card.getActive()).isTrue();
    }

    @Test
    void deactivateCard_shouldReturnNoContent() throws Exception {
        CreatePaymentCardRequest cardRequest = CreatePaymentCardRequest.builder()
                .holder("Card Owner")
                .build();

        String response = mockMvc.perform(post("/api/v1/users/{publicUserId}/cards", userPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        PaymentCardResponse createdCard = objectMapper.readValue(response, PaymentCardResponse.class);
        Long cardId = createdCard.getId();

        mockMvc.perform(patch("/api/v1/users/{publicUserId}/cards/{cardId}/deactivate", userPublicId, cardId))
                .andExpect(status().isNoContent());

        String getResponse = mockMvc.perform(get("/api/v1/users/{publicUserId}/cards/{cardId}", userPublicId, cardId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        PaymentCardResponse card = objectMapper.readValue(getResponse, PaymentCardResponse.class);
        assertThat(card.getActive()).isFalse();
    }
}