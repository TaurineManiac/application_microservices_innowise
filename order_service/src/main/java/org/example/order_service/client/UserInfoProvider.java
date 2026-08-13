package org.example.order_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.dto.UserInfo;
import org.springframework.retry.annotation.Backoff;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoProvider {

    private final UserServiceClient userServiceClient;

    @Retryable(
            value = {Exception.class},
            maxAttemptsExpression = "${retry.maxAttempts}",
            backoff = @Backoff(
                    delayExpression = "${retry.delay}",
                    multiplierExpression = "${retry.multiplier}"
            )
    )
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserInfo")
    public UserInfo getUserInfo(UUID publicId){
        log.debug("Calling User Service for publicId: {}", publicId);
        return userServiceClient.getUserByPublicId(publicId);
    }

    public UserInfo fallbackGetUserInfo(UUID publicId, Throwable throwable) {
        log.warn("Fallback triggered for publicId: {}. Reason: {}", publicId, throwable.getMessage());
        return UserInfo.builder()
                .publicId(publicId)
                .name("Unknown")
                .surname("User")
                .email("unknown@system.com")
                .build();
    }
}
