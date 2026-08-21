package org.example.api_gateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.example.api_gateway.filter.JwtAuthenticationGlobalFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGlobalFilterTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static MockWebServer authServer;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthenticationGlobalFilter filter;

    @BeforeAll
    static void setUpServer() throws IOException {
        authServer = new MockWebServer();
        authServer.start();
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        authServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        WebClient.Builder webClientBuilder = WebClient.builder();
        filter = new JwtAuthenticationGlobalFilter(
                webClientBuilder,
                List.of(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh"
                ),
                authServer.url("/").toString()
        );
    }

    @Test
    void shouldSkipJwtValidationForPublicEndpoint() {
        var request = MockServerHttpRequest
                .get("/api/auth/login")
                .build();
        var exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderIsMissing() {
        var request = MockServerHttpRequest
                .get("/api/v1/users")
                .build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderDoesNotContainBearer() {
        var request = MockServerHttpRequest
                .get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Basic some-token")
                .build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowRequestWhenJwtIsValid() {
        authServer.enqueue(
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

        var request = MockServerHttpRequest
                .get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        var exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // Используем ArgumentCaptor, чтобы проверить реально переданный exchange
        ArgumentCaptor<ServerWebExchange> captor =
                ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(filterChain).filter(captor.capture());

        ServerWebExchange capturedExchange = captor.getValue();

        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-UUID"))
                .isEqualTo(USER_ID.toString());
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("USER");
    }

    @Test
    void shouldReturnUnauthorizedWhenJwtIsInvalid() {
        authServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "valid": false
                                }
                                """)
        );

        var request = MockServerHttpRequest
                .get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldReturnServiceUnavailableWhenAuthenticationServiceReturnsUnauthorized() {
        // Auth Server отвечает 401, WebClient бросает исключение
        authServer.enqueue(new MockResponse().setResponseCode(401));

        var request = MockServerHttpRequest
                .get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // Согласно текущей реализации фильтра, любая ошибка от Auth Server превращается в 503
        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldReturnServiceUnavailableWhenAuthenticationServiceIsUnavailable()
            throws IOException {
        authServer.shutdown();

        var request = MockServerHttpRequest
                .get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(filterChain);

        // Перезапускаем сервер для следующих тестов
        authServer = new MockWebServer();
        authServer.start();

        WebClient.Builder webClientBuilder = WebClient.builder();
        filter = new JwtAuthenticationGlobalFilter(
                webClientBuilder,
                List.of("/api/auth/login", "/api/auth/register", "/api/auth/refresh"),
                authServer.url("/").toString()
        );
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}