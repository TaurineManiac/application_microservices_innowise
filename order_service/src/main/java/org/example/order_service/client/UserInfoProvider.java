package org.example.order_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.order_service.dto.UserInfo;
import org.example.order_service.util.SecurityUtils;
import org.springframework.retry.annotation.Backoff;
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
            maxAttemptsExpression = "${retry.maxAttempts:3}",
            backoff = @Backoff(
                    delayExpression = "${retry.delay:500}",
                    multiplierExpression = "${retry.multiplier:2}"
            )
    )
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserInfo")
    public UserInfo getUserInfo(UUID publicId) {
        UUID currentUserId = SecurityUtils.getCurrentUserPublicId();
        String role = SecurityUtils.getCurrentUserRole();
        return userServiceClient.getUserByPublicId(publicId, currentUserId.toString(), role);
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