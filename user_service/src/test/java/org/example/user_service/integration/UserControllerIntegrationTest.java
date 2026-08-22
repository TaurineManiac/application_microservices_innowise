package org.example.user_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redis.testcontainers.RedisContainer;
import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.UpdateUserRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
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
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
@WithMockUser(roles = "ADMIN")
@EmbeddedKafka(partitions = 1, topics = {"user-status-events"})
class UserControllerIntegrationTest {

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
        registry.add("internal.service.token", () -> "test-token");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private CreateUserRequest createRequest;

    @BeforeEach
    void setUp() {

        userRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        createRequest = CreateUserRequest.builder()
                .name("Integration")
                .surname("Test")
                .email("integration@test.com")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .build();
    }

    @Test
    void createUser_shouldReturnCreated() throws Exception {
        String json = objectMapper.writeValueAsString(createRequest);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.email").value("integration@test.com"));
    }

    @Test
    void getUserByPublicId_shouldReturnUser() throws Exception {
        String json = objectMapper.writeValueAsString(createRequest);
        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(response, UserResponse.class);
        UUID publicId = created.getPublicId();

        mockMvc.perform(get("/api/v1/users/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.email").value("integration@test.com"));
    }

    @Test
    void updateUser_shouldUpdateAndReturnOk() throws Exception {
        String json = objectMapper.writeValueAsString(createRequest);
        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(response, UserResponse.class);
        UUID publicId = created.getPublicId();

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .name("Updated")
                .surname("Name")
                .email("updated@test.com")
                .build();

        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/api/v1/users/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.surname").value("Name"))
                .andExpect(jsonPath("$.email").value("updated@test.com"));
    }

    @Test
    void activateUser_shouldReturnNoContent() throws Exception {
        String json = objectMapper.writeValueAsString(createRequest);
        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(response, UserResponse.class);
        UUID publicId = created.getPublicId();

        mockMvc.perform(patch("/api/v1/users/{publicId}/deactivate", publicId))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/users/{publicId}/activate", publicId))
                .andExpect(status().isNoContent());

        String getResponse = mockMvc.perform(get("/api/v1/users/{publicId}", publicId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UserResponse user = objectMapper.readValue(getResponse, UserResponse.class);
        assertThat(user.getActive()).isTrue();
    }

    @Test
    void deactivateUser_shouldReturnNoContent() throws Exception {
        String json = objectMapper.writeValueAsString(createRequest);
        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse created = objectMapper.readValue(response, UserResponse.class);
        UUID publicId = created.getPublicId();

        mockMvc.perform(patch("/api/v1/users/{publicId}/deactivate", publicId))
                .andExpect(status().isNoContent());

        String getResponse = mockMvc.perform(get("/api/v1/users/{publicId}", publicId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UserResponse user = objectMapper.readValue(getResponse, UserResponse.class);
        assertThat(user.getActive()).isFalse();
    }

    @Test
    void getAllUsers_shouldReturnFilteredPage() throws Exception {
        CreateUserRequest user1 = CreateUserRequest.builder()
                .name("Alice")
                .surname("Smith")
                .email("alice@test.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        CreateUserRequest user2 = CreateUserRequest.builder()
                .name("Bob")
                .surname("Smith")
                .email("bob@test.com")
                .dateOfBirth(LocalDate.of(1985, 2, 2))
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users")
                        .param("surname", "Smith")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").exists());
    }
}