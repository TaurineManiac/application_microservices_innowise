package org.example.authentication_service.service;

import org.example.authentication_service.dto.CreateUserRequest;
import org.example.authentication_service.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {

    @PostMapping("/api/v1/users")
    UserResponse createUser(@RequestBody CreateUserRequest request);

    @DeleteMapping("/api/v1/users/internal/{publicId}")
    void rollbackUser(@PathVariable("publicId") UUID publicId,
                      @RequestHeader("X-Internal-Token") String internalToken);
}