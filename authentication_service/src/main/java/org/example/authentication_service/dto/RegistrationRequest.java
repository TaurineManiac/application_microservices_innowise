package org.example.authentication_service.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistrationRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String surname;

    @NotBlank
    private String password;

    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Past
    @NotNull
    private LocalDate dateOfBirth;
}
