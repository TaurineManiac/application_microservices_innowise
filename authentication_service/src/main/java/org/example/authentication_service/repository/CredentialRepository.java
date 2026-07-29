package org.example.authentication_service.repository;

import jakarta.validation.constraints.Email;
import org.example.authentication_service.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByEmail(@Email String email);
    Optional<Credential> findByPublicId(UUID publicId);
}
