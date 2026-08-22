package org.example.order_service.client;

import org.example.order_service.config.FeignClientConfig;
import org.example.order_service.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${user.service.url:http://localhost:8080}",
        configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/v1/users/{publicId}")
    UserInfo getUserByPublicId(@PathVariable("publicId") UUID publicId);
}