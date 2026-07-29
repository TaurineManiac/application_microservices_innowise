package org.example.authentication_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.authentication_service.enums.Role;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@Entity
@Table(name = "credentials")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,  nullable = false)
    private UUID publicId;

    @Email
    @Column(unique = true,  nullable = false)
    private String email;

    @Column(unique = true,  nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(unique = true,  nullable = false)
    private Role role;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "credential", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST,  CascadeType.REFRESH})
    private HashSet<RefreshToken> refreshTokens;
}
