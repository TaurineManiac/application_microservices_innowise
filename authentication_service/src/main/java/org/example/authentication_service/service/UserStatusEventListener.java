package org.example.authentication_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authentication_service.dto.UserStatusEvent;
import org.example.authentication_service.entity.Credential;
import org.example.authentication_service.entity.RefreshToken;
import org.example.authentication_service.repository.CredentialRepository;
import org.example.authentication_service.repository.RefreshTokenRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserStatusEventListener {

    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @KafkaListener(topics = "user-status-events")
    @Transactional
    public void handleUserStatusEvent(UserStatusEvent event) {
        log.info("Received user status event: {}", event);
        UUID publicId = event.getPublicId();

        Credential credential = credentialRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Credential not found for publicId: " + publicId));
        credential.setActive(event.getActive());
        credentialRepository.save(credential);
        log.info("Updated active status for user {} to {}", publicId, event.getActive());


        if (!event.getActive()) {
            List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByCredentialPublicId(publicId);
            refreshTokens.forEach(token -> token.setRevoked(true));
            refreshTokenRepository.saveAll(refreshTokens);
            log.info("Revoked {} refresh tokens for user {}", refreshTokens.size(), publicId);
        }
    }
}