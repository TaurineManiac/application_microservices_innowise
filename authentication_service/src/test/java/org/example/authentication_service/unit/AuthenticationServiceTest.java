package org.example.authentication_service.unit;

import org.example.authentication_service.config.KafkaTestConfig;
import org.example.authentication_service.config.TestApplicationContext;
import org.example.authentication_service.dto.*;
import org.example.authentication_service.entity.Credential;
import org.example.authentication_service.entity.RefreshToken;
import org.example.authentication_service.enums.Role;
import org.example.authentication_service.exception.EmailAlreadyExistsException;
import org.example.authentication_service.exception.InvalidCredentialException;
import org.example.authentication_service.exception.InvalidTokenException;
import org.example.authentication_service.repository.CredentialRepository;
import org.example.authentication_service.repository.RefreshTokenRepository;
import org.example.authentication_service.service.AuthenticationService;
import org.example.authentication_service.service.JwtService;
import org.example.authentication_service.service.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AuthenticationService authenticationService;

    private Credential credential;
    private LoginRequest loginRequest;
    private RegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "refreshExpirationMs", 604_800_000L);

        credential = Credential.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .email("john@test.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        loginRequest = new LoginRequest("john@test.com", "password123");

        registrationRequest = new RegistrationRequest(
                "John",
                "Doe",
                "password123",
                "john@test.com",
                LocalDate.of(1990, 1, 1)
        );
    }


    @Test
    void login_shouldReturnTokens_whenCredentialsValid() {
        when(credentialRepository.findByEmail("john@test.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(credential.getPublicId(), credential.getRole())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(credential.getPublicId())).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthenticationResponse response = authenticationService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("refresh-token");
        assertThat(captor.getValue().getCredential()).isEqualTo(credential);
        assertThat(captor.getValue().getRevoked()).isFalse();
    }

    @Test
    void login_shouldThrowInvalidCredentialException_whenEmailNotFound() {
        when(credentialRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("Invalid email or password");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_shouldThrowInvalidCredentialException_whenPasswordWrong() {
        when(credentialRepository.findByEmail("john@test.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateAccessToken(any(), any());
    }


    @Test
    void refresh_shouldReturnNewTokens_andRevokeOldOne_whenValid() {
        RefreshToken existingToken = RefreshToken.builder()
                .id(1L)
                .credential(credential)
                .token("old-refresh-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(existingToken));
        when(jwtService.generateAccessToken(credential.getPublicId(), credential.getRole())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(credential.getPublicId())).thenReturn("new-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthenticationResponse response = authenticationService.refresh("old-refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(existingToken.getRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldThrowInvalidTokenException_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refresh("unknown-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_shouldThrowInvalidTokenException_whenTokenRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .credential(credential)
                .token("revoked-token")
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authenticationService.refresh("revoked-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void refresh_shouldThrowInvalidTokenException_whenTokenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .credential(credential)
                .token("expired-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authenticationService.refresh("expired-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }


    @Test
    void validateToken_shouldReturnValid_whenTokenIsValid() {
        String token = "valid-token";
        UUID publicId = UUID.randomUUID();

        Credential credential = Credential.builder()
                .publicId(publicId)
                .active(true)
                .build();

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractPublicId(token)).thenReturn(publicId);
        when(jwtService.extractRole(token)).thenReturn("USER");
        when(credentialRepository.findByPublicId(publicId)).thenReturn(Optional.of(credential));

        ValidateResponse response = authenticationService.validateToken(token);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getPublicId()).isEqualTo(publicId);
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void validateToken_shouldReturnInvalid_whenTokenIsNotValid() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        ValidateResponse response = authenticationService.validateToken("bad-token");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getPublicId()).isNull();
        verify(jwtService, never()).extractPublicId(anyString());
    }

    @Test
    void validateToken_shouldReturnInvalid_whenExtractionThrows() {
        String token = "valid-format-but-broken";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractPublicId(token)).thenThrow(new InvalidTokenException("Invalid UUID in token subject"));

        ValidateResponse response = authenticationService.validateToken(token);

        assertThat(response.isValid()).isFalse();
    }


    @Test
    void registration_shouldCreateCredential_whenEmailNotTaken() {
        UUID newPublicId = UUID.randomUUID();
        UserResponse userResponse = UserResponse.builder()
                .publicId(newPublicId)
                .email("john@test.com")
                .name("John")
                .surname("Doe")
                .build();

        when(credentialRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());
        when(userServiceClient.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password123");
        when(credentialRepository.save(any(Credential.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = authenticationService.registration(registrationRequest);

        assertThat(result).isEqualTo(userResponse);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getPublicId()).isEqualTo(newPublicId);
        assertThat(captor.getValue().getEmail()).isEqualTo("john@test.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password123");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registration_shouldThrowEmailAlreadyExistsException_whenEmailTaken() {
        when(credentialRepository.findByEmail("john@test.com")).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> authenticationService.registration(registrationRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("john@test.com");

        verify(userServiceClient, never()).createUser(any());
        verify(credentialRepository, never()).save(any());
    }
}