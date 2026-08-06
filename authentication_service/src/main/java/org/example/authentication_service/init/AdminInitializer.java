package org.example.authentication_service.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authentication_service.repository.CredentialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "admin.init.enabled", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements ApplicationRunner {

    private final CredentialRepository credentialRepository;
    private final AdminUserProvisioner provisioner;

    @Value("${admin.email:admin@system.com}") private String adminEmail;
    @Value("${admin.password:admin}") private String adminPassword;
    @Value("${admin.name:Admin}") private String adminName;
    @Value("${admin.surname:Admin}") private String adminSurname;

    @Override
    public void run(ApplicationArguments args) {
        if (credentialRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin already exists. Skipping initialization.");
            return;
        }
        provisioner.createAdminUser(adminEmail, adminPassword, adminName, adminSurname);
    }
}