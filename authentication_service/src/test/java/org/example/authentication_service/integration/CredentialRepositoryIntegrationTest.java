package org.example.authentication_service.integration;

import org.example.authentication_service.config.KafkaTestConfig;
import org.example.authentication_service.config.TestApplicationContext;
import org.example.authentication_service.entity.Credential;
import org.example.authentication_service.enums.Role;
import org.example.authentication_service.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApplicationContext.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Transactional
@Testcontainers
@Import(KafkaTestConfig.class)
public class CredentialRepositoryIntegrationTest extends IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "dGVzdC1zZWNyZXQta2V5LWZvci1qd3Qtc2lnbmluZy10ZXN0cy0xMjM0NTY=");
    }

    @Autowired
    private CredentialRepository credentialRepository;

    @BeforeEach
    void setUp() {
        credentialRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindCredentialByEmail() {
        Credential credential = Credential.builder()
                .publicId(UUID.randomUUID())
                .email("john@test.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .build();

        credentialRepository.save(credential);

        Optional<Credential> found = credentialRepository.findByEmail("john@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldFindCredentialByPublicId() {
        UUID publicId = UUID.randomUUID();
        Credential credential = Credential.builder()
                .publicId(publicId)
                .email("jane@test.com")
                .passwordHash("hashed-password")
                .role(Role.ADMIN)
                .build();

        credentialRepository.save(credential);

        Optional<Credential> found = credentialRepository.findByPublicId(publicId);
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("jane@test.com");
    }

    @Test
    void shouldReturnEmpty_whenEmailNotFound() {
        Optional<Credential> found = credentialRepository.findByEmail("missing@test.com");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldPopulateCreatedAtAndUpdatedAt_onSave() {
        Credential credential = Credential.builder()
                .publicId(UUID.randomUUID())
                .email("audit@test.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .build();

        Credential saved = credentialRepository.save(credential);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldEnforceUniqueEmailConstraint() {
        Credential first = Credential.builder()
                .publicId(UUID.randomUUID())
                .email("duplicate@test.com")
                .passwordHash("hash-1")
                .role(Role.USER)
                .build();
        credentialRepository.saveAndFlush(first);

        Credential second = Credential.builder()
                .publicId(UUID.randomUUID())
                .email("duplicate@test.com")
                .passwordHash("hash-2")
                .role(Role.USER)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> credentialRepository.saveAndFlush(second))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}