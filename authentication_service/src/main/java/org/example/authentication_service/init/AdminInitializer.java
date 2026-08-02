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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final CredentialRepository credentialRepository;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@system.com}")
    private String adminEmail;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Value("${admin.name:Admin}")
    private String adminName;

    @Value("${admin.surname:Admin}")
    private String adminSurname;

    @Override
    public void run(ApplicationArguments args) {
        if (credentialRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin already exists. Skipping initialization.");
            return;
        }

        try {
            log.info("Creating admin user...");

            CreateUserRequest createUserRequest = CreateUserRequest.builder()
                    .name(adminName)
                    .surname(adminSurname)
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .email(adminEmail)
                    .build();

            UserResponse userResponse = userServiceClient.createUser(createUserRequest);
            log.info("Admin user created in User Service with publicId: {}", userResponse.getPublicId());

            Credential credential = Credential.builder()
                    .publicId(userResponse.getPublicId())
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();

            credentialRepository.save(credential);
            log.info("Admin credentials saved successfully.");
            log.info("Admin email: {}, password: {}", adminEmail, adminPassword);
        } catch (Exception e) {
            log.error("Failed to bootstrap admin: {}", e.getMessage(), e);
            // Не прерываем запуск, просто логируем ошибку. Админа можно будет создать позже вручную.
        }
    }
}