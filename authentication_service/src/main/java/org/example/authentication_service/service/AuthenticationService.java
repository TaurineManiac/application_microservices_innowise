package org.example.authentication_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authentication_service.dto.*;
import org.example.authentication_service.entity.Credential;
import org.example.authentication_service.entity.RefreshToken;
import org.example.authentication_service.enums.Role;
import org.example.authentication_service.exception.EmailAlreadyExistsException;
import org.example.authentication_service.exception.InvalidCredentialException;
import org.example.authentication_service.exception.InvalidTokenException;
import org.example.authentication_service.repository.CredentialRepository;
import org.example.authentication_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationService {

    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpirationMs;

    public AuthenticationResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        Credential credential = credentialRepository.findByEmail(loginRequest.getEmail()).orElseThrow(
                () -> new InvalidCredentialException("Invalid email or password")
        );

        if(!passwordEncoder.matches(loginRequest.getPassword(), credential.getPasswordHash())) {
            throw new InvalidCredentialException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(credential.getPublicId(), credential.getRole());
        String refreshToken = jwtService.generateRefreshToken(credential.getPublicId());

        saveRefreshToken(refreshToken, credential);

        log.info("User logged in successfully: {}", credential.getEmail());
        return new AuthenticationResponse(accessToken, refreshToken);
    }

    private void saveRefreshToken(String refreshToken, Credential credential) {
        RefreshToken refreshTokenEntity= RefreshToken.builder()
                .token(refreshToken)
                .credential(credential)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);
        log.debug("Refresh token saved for user: {}", credential.getEmail());
    }

    public AuthenticationResponse refresh(String refreshToken) {

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken).orElseThrow(
                () -> new InvalidTokenException("Invalid refresh token")
        );

        if(refreshTokenEntity.getRevoked()){
            throw new InvalidTokenException("Refresh token is revoked");
        }

        if(refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new InvalidTokenException("Refresh token is expired");
        }

        refreshTokenEntity.setRevoked(true);
        refreshTokenRepository.save(refreshTokenEntity);

        String accessToken = jwtService.generateAccessToken(
                refreshTokenEntity.getCredential().getPublicId(),
                refreshTokenEntity.getCredential().getRole()
        );

        String newRefreshTokenEntity = jwtService.generateRefreshToken(
                refreshTokenEntity.getCredential().getPublicId()
        );

        saveRefreshToken(newRefreshTokenEntity, refreshTokenEntity.getCredential());

        return new AuthenticationResponse(accessToken, newRefreshTokenEntity);
    }

    public ValidateResponse validateToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            return new ValidateResponse(null, null, false);
        }
        try {
            UUID publicId = jwtService.extractPublicId(token);
            String role = jwtService.extractRole(token);
            return new ValidateResponse(publicId, role, true);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return new ValidateResponse(null, null, false);
        }
    }

    public UserResponse registration(RegistrationRequest request){
        if(credentialRepository.findByEmail(request.getEmail()).isPresent()){
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .email(request.getEmail())
                .dateOfBirth(request.getDateOfBirth())
                .build();

        UserResponse userResponse = userServiceClient.createUser(createUserRequest);
        log.info("User created successfully: {}", userResponse.getEmail());

        Credential credential = Credential.builder()
                .publicId(userResponse.getPublicId())
                .email(userResponse.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        credentialRepository.save(credential);
        return userResponse;
    }
}
