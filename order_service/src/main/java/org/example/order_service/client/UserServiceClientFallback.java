package org.example.order_service.client;

import lombok.extern.slf4j.Slf4j;
import org.example.order_service.dto.UserInfo;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserInfo getUserByPublicId(UUID publicId) {
        log.warn("User Service unavailable, returning fallback for user: {}", publicId);
        return UserInfo.builder()
                .publicId(publicId)
                .name("Unknown")
                .surname("User")
                .email("unknown@system.com")
                .build();
    }
}