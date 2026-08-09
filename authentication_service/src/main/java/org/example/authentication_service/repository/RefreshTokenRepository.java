package org.example.authentication_service.repository;

import org.example.authentication_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByCredentialPublicId(UUID publicId);
}
