package org.example.api_gateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayRoutingIntegrationTest {

    private static MockWebServer mockBackend;

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private WebTestClient webTestClient;

    private final UUID userId =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeAll
    static void setUpServer() throws IOException {
        mockBackend = new MockWebServer();
        mockBackend.start();
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        mockBackend.shutdown();
    }

    @BeforeEach
    void resetServer() throws InterruptedException {
        while (mockBackend.takeRequest(100, TimeUnit.MILLISECONDS) != null) {
            // очищаем предыдущие requests
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        String backendUrl = mockBackend.url("/").toString();

        registry.add("AUTH_SERVICE_URL", () -> backendUrl);
        registry.add("USER_SERVICE_URL", () -> backendUrl);
        registry.add("ORDER_SERVICE_URL", () -> backendUrl);

        registry.add(
                "AUTH_SERVICE_PREDICATE_LOGIN",
                () -> "/api/auth/login"
        );

        registry.add(
                "AUTH_SERVICE_PREDICATE_REFRESH",
                () -> "/api/auth/refresh"
        );

        registry.add(
                "AUTH_SERVICE_PREDICATE_REGISTER",
                () -> "/api/auth/register"
        );

        registry.add(
                "USER_SERVICE_PREDICATE",
                () -> "/api/v1/users/**"
        );

        registry.add(
                "ORDER_SERVICE_PREDICATE",
                () -> "/api/v1/orders/**"
        );
    }

    @Test
    void shouldRouteLoginRequestToAuthService() throws Exception {

        mockBackend.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "message": "login successful"
                                }
                                """)
        );

        webTestClient
                .post()
                .uri("/api/auth/login")
                .header("Content-Type", "application/json")
                .bodyValue("""
                        {
                          "email": "test@example.com",
                          "password": "password"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json("""
                        {
                          "message": "login successful"
                        }
                        """);

        RecordedRequest request =
                mockBackend.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/auth/login");
    }

    @Test
    void shouldRouteRegisterRequestToAuthService() throws Exception {

        mockBackend.enqueue(
                new MockResponse()
                        .setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "message": "registration successful"
                                }
                                """)
        );

        webTestClient
                .post()
                .uri("/api/auth/register")
                .header("Content-Type", "application/json")
                .bodyValue("""
                        {
                          "email": "test@example.com",
                          "password": "password",
                          "name": "John",
                          "surname": "Doe"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .json("""
                        {
                          "message": "registration successful"
                        }
                        """);

        RecordedRequest request =
                mockBackend.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/auth/register");
    }

    @Test
    void shouldValidateJwtAndRouteRequestToUserService() throws Exception {

        mockBackend.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "valid": true,
                                  "publicId": "11111111-1111-1111-1111-111111111111",
                                  "role": "USER"
                                }
                                """)
        );

        mockBackend.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "id": "123",
                                  "name": "John",
                                  "surname": "Doe"
                                }
                                """)
        );

        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json("""
                        {
                          "id": "123",
                          "name": "John",
                          "surname": "Doe"
                        }
                        """);

        RecordedRequest validationRequest =
                mockBackend.takeRequest(1, TimeUnit.SECONDS);

        assertThat(validationRequest).isNotNull();
        assertThat(validationRequest.getMethod()).isEqualTo("GET");
        assertThat(validationRequest.getPath())
                .isEqualTo("/api/auth/validate");
        assertThat(validationRequest.getHeader("Authorization"))
                .isEqualTo("Bearer valid-token");

        RecordedRequest userRequest =
                mockBackend.takeRequest(1, TimeUnit.SECONDS);

        assertThat(userRequest).isNotNull();
        assertThat(userRequest.getMethod()).isEqualTo("GET");
        assertThat(userRequest.getPath()).isEqualTo("/api/v1/users/me");

        assertThat(userRequest.getHeader("X-User-UUID"))
                .isEqualTo(userId.toString());

        assertThat(userRequest.getHeader("X-User-Role"))
                .isEqualTo("USER");
    }

    @Test
    void shouldRouteOrderRequestToOrderService() throws Exception {

        mockBackend.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "valid": true,
                                  "publicId": "11111111-1111-1111-1111-111111111111",
                                  "role": "USER"
                                }
                                """)
        );

        mockBackend.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "id": 10,
                                  "status": "CREATED"
                                }
                                """)
        );

        webTestClient
                .get()
                .uri("/api/v1/orders/10")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json("""
                        {
                          "id": 10,
                          "status": "CREATED"
                        }
                        """);

        RecordedRequest validationRequest =
                mockBackend.takeRequest(1, TimeUnit.SECONDS);

        assertThat(validationRequest).isNotNull();
        assertThat(validationRequest.getPath())
                .isEqualTo("/api/auth/validate");

        RecordedRequest orderRequest =
                mockBackend.takeRequest(1, TimeUnit.SECONDS);

        assertThat(orderRequest).isNotNull();
        assertThat(orderRequest.getMethod()).isEqualTo("GET");
        assertThat(orderRequest.getPath())
                .isEqualTo("/api/v1/orders/10");

        assertThat(orderRequest.getHeader("X-User-UUID"))
                .isEqualTo(userId.toString());

        assertThat(orderRequest.getHeader("X-User-Role"))
                .isEqualTo("USER");
    }

    @Test
    void shouldReturn401WhenProtectedRouteHasNoToken() {

        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void shouldReturn401WhenJwtIsInvalid() throws Exception {

        mockBackend.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "valid": false
                                }
                                """)
        );

        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer invalid-token")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        RecordedRequest request =
                mockBackend.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getPath())
                .isEqualTo("/api/auth/validate");

        assertThat(request.getHeader("Authorization"))
                .isEqualTo("Bearer invalid-token");
    }
}