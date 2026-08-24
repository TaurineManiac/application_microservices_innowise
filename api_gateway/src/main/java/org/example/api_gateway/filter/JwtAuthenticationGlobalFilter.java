package org.example.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.api_gateway.dto.ValidateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    private List<String> publicPaths;

    public JwtAuthenticationGlobalFilter(
            WebClient.Builder webClientBuilder,
            @Value("#{'${gateway.public.paths}'.split(',')}") List<String> publicPaths,
            @Value("${auth.service.url:http://localhost:8081}") String authServiceUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
        this.publicPaths = publicPaths.stream().map(String::trim).toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if(publicPaths.contains(path)) {
            log.debug("Public endpoint: {}, skipping JWT validation", path);
            return chain.filter(exchange);
        }

        String authorizationHeader = request.getHeaders().getFirst("Authorization");
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authorizationHeader.substring(7);

        return validateTokenAndForward(exchange,chain,token,path);
    }

    private Mono<Void> validateTokenAndForward(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String token,
            String path
    ){
        return webClient.get()
                .uri("/api/auth/validate")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(ValidateResponse.class)
                .flatMap(response -> {
                    if (!response.isValid()) {
                        log.warn("Invalid token for path: {}", path);
                        return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                    }
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .headers(httpHeaders -> {
                                httpHeaders.remove("X-User-UUID");
                                httpHeaders.remove("X-User-Role");
                            })
                            .header("X-User-UUID", response.getPublicId().toString())
                            .header("X-User-Role", response.getRole())
                            .build();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(mutatedRequest)
                            .build();

                    log.debug("Token validated for user: {}, role: {}", response.getPublicId(), response.getRole());
                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(e -> {
                    log.error("Error validating token: {}", e.getMessage());
                    return onError(
                            exchange,
                            "Authentication service unavailable",
                            HttpStatus.SERVICE_UNAVAILABLE
                    );
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add(
                "Content-Type", "application/json"
        );

        String body = String.format(
                "{\"error\": \"%s\", \"status\": %d}", message, status.value()
        );

        byte[] bites = body.getBytes();
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bites))
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
