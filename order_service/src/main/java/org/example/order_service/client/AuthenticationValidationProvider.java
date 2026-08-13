package org.example.order_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.dto.ValidateResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationValidationProvider {

    private final AuthenticationServiceClient authServiceClient;

    @Retryable(
            value = {Exception.class},
            maxAttemptsExpression = "${retry.maxAttempts}",
            backoff = @Backoff(
                    delayExpression = "${retry.delay}",
                    multiplierExpression = "${retry.multiplier}"
            )
    )
    @CircuitBreaker(name = "authService", fallbackMethod = "fallbackValidate")
    public ValidateResponse validateToken(String token) {
        log.debug("Validating token via Auth Service");
        return authServiceClient.validateToken(token);
    }

    public ValidateResponse fallbackValidate(String token, Throwable throwable) {
        log.warn("Fallback for token validation: {}", throwable.getMessage());
        return new ValidateResponse(null, null, false);
    }
}
