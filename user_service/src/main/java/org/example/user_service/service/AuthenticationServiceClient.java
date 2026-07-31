package org.example.user_service.service;

import org.example.user_service.dto.ValidateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthenticationServiceClient {

    @GetMapping("/api/auth/validate")
    ValidateResponse validateToken(@RequestHeader("Authorization") String token);
}