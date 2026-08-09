package org.example.authentication_service.integration;

import org.example.authentication_service.config.KafkaTestConfig;
import org.example.authentication_service.config.TestApplicationContext;
import org.example.authentication_service.entity.Credential;
import org.example.authentication_service.entity.RefreshToken;
import org.example.authentication_service.enums.Role;
import org.example.authentication_service.repository.CredentialRepository;
import org.example.authentication_service.repository.RefreshTokenRepository;
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

import java.time.LocalDateTime;
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
public class RefreshTokenRepositoryIntegrationTest extends IntegrationTestBase {

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    private Credential credential;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        credentialRepository.deleteAll();

        credential = Credential.builder()
                .publicId(UUID.randomUUID())
                .email("john@test.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .build();
        credential = credentialRepository.save(credential);
    }

    @Test
    void shouldSaveAndFindRefreshTokenByToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .credential(credential)
                .token("test-refresh-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);

        Optional<RefreshToken> found = refreshTokenRepository.findByToken("test-refresh-token");
        assertThat(found).isPresent();
        assertThat(found.get().getCredential().getEmail()).isEqualTo("john@test.com");
        assertThat(found.get().getRevoked()).isFalse();
    }

    @Test
    void shouldReturnEmpty_whenTokenNotFound() {
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent-token");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldPersistRevokedFlag_afterUpdate() {
        RefreshToken refreshToken = RefreshToken.builder()
                .credential(credential)
                .token("revoke-me-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);

        RefreshToken saved = refreshTokenRepository.findByToken("revoke-me-token").orElseThrow();
        saved.setRevoked(true);
        refreshTokenRepository.save(saved);

        RefreshToken reloaded = refreshTokenRepository.findByToken("revoke-me-token").orElseThrow();
        assertThat(reloaded.getRevoked()).isTrue();
    }

    @Test
    void shouldEnforceUniqueTokenConstraint() {
        RefreshToken first = RefreshToken.builder()
                .credential(credential)
                .token("duplicate-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.saveAndFlush(first);

        RefreshToken second = RefreshToken.builder()
                .credential(credential)
                .token("duplicate-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(second))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}