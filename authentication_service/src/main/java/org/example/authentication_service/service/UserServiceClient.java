package org.example.authentication_service.service;

import org.example.authentication_service.dto.CreateUserRequest;
import org.example.authentication_service.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {

    @PostMapping("/api/v1/users")
    UserResponse createUser(@RequestBody CreateUserRequest request);
}