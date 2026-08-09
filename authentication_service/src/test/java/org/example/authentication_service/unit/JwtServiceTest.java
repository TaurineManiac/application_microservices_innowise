package org.example.authentication_service.unit;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.authentication_service.enums.Role;
import org.example.authentication_service.exception.InvalidTokenException;
import org.example.authentication_service.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3Qtc2lnbmluZy10ZXN0cy0xMjM0NTY=";

    private static final String OTHER_SECRET =
            "b3RoZXItc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmctdGVzdHMtNjU0MzIx";

    private static final long ACCESS_EXPIRATION_MS = 900_000L;
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = createJwtService(SECRET);
    }

    private JwtService createJwtService(String secret) {
        JwtService service = new JwtService();

        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(
                service,
                "accessExpirationMs",
                ACCESS_EXPIRATION_MS
        );
        ReflectionTestUtils.setField(
                service,
                "refreshExpirationMs",
                REFRESH_EXPIRATION_MS
        );

        return service;
    }

    @Test
    void generateAccessToken_shouldBeValidAndContainPublicIdAndRole() {
        UUID publicId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(publicId, Role.USER);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractPublicId(token)).isEqualTo(publicId);
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void generateRefreshToken_shouldBeValidAndContainPublicId() {
        UUID publicId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(publicId);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractPublicId(token)).isEqualTo(publicId);
        assertThat(jwtService.extractRole(token)).isNull();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsMalformed() {
        assertThat(jwtService.isTokenValid("not-a-valid-jwt")).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenSignedWithDifferentKey() {
        UUID publicId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(
                publicId,
                Role.USER
        );

        JwtService otherService = createJwtService(OTHER_SECRET);

        assertThat(otherService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        SecretKey key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(SECRET)
        );

        long now = System.currentTimeMillis();

        String expiredToken = Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .setIssuedAt(new Date(now - 10_000))
                .setExpiration(new Date(now - 5_000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void extractPublicId_shouldThrowInvalidTokenException_whenSubjectIsNotUuid() {
        SecretKey key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(SECRET)
        );

        String tokenWithBadSubject = Jwts.builder()
                .setSubject("not-a-uuid")
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_MS)
                )
                .signWith(key)
                .compact();

        assertThatThrownBy(
                () -> jwtService.extractPublicId(tokenWithBadSubject)
        )
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid UUID in token subject");
    }

    @Test
    void extractRole_shouldReturnNull_whenTokenHasNoRoleClaim() {
        UUID publicId = UUID.randomUUID();

        String refreshToken = jwtService.generateRefreshToken(publicId);

        assertThat(jwtService.extractRole(refreshToken)).isNull();
    }
}