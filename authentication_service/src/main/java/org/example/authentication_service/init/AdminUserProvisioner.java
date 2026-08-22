package org.example.authentication_service.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authentication_service.dto.CreateUserRequest;
import org.example.authentication_service.dto.UserResponse;
import org.example.authentication_service.entity.Credential;
import org.example.authentication_service.enums.Role;
import org.example.authentication_service.repository.CredentialRepository;
import org.example.authentication_service.service.UserServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserProvisioner {

    @Value("${internal.service.token}")
    private String internalServiceToken;
    private final CredentialRepository credentialRepository;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;

    @Retryable(
            retryFor = Exception.class,
            maxAttemptsExpression = "#{${admin.retry.maxAttempts:10}}",
            backoff = @Backoff(delayExpression = "#{${admin.retry.delay:5000}}")
    )
    public UserResponse createAdminUser(String email, String password, String name, String surname) {
        CreateUserRequest request = CreateUserRequest.builder()
                .name(name).surname(surname)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email(email)
                .build();

        UserResponse userResponse = userServiceClient.createUser(request, internalServiceToken);

        Credential credential = Credential.builder()
                .publicId(userResponse.getPublicId())
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();

        credentialRepository.save(credential);
        return userResponse;
    }

    @Recover
    public UserResponse recover(Exception exception, String email, String password, String name, String surname) {
        log.error("Failed to provision admin after retries: ", exception);
        return null;
    }
}
