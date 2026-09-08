package org.example.order_service.client;

import org.example.order_service.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/{publicId}")
    UserInfo getUserByPublicId(
            @PathVariable("publicId") UUID publicId,
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestHeader("X-User-Role") String userRole
    );
}