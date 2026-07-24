package org.example.user_service.unit;

import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.UpdateUserRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.entity.User;
import org.example.user_service.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = UserMapper.INSTANCE;

    @Test
    void shouldMapEntityToResponse() {
        User user = User.builder()
                .id(1L)
                .publicId("pub-123")
                .name("John")
                .surname("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@test.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserResponse response = mapper.toUserResponse(user);

        assertThat(response).isNotNull();
        assertThat(response.getPublicId()).isEqualTo("pub-123");
        assertThat(response.getName()).isEqualTo("John");
    }

    @Test
    void shouldMapCreateRequestToEntity() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("Jane")
                .surname("Smith")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .email("jane@test.com")
                .build();

        User user = mapper.toEntity(request);

        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("Jane");
        assertThat(user.getActive()).isTrue();
        assertThat(user.getId()).isNull();
    }

    @Test
    void shouldUpdateEntityFromUpdateRequest() {
        User user = User.builder()
                .id(1L)
                .name("Old")
                .surname("User")
                .email("old@test.com")
                .build();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("New")
                .surname("Name")
                .email("new@test.com")
                .build();

        mapper.updateEntity(request, user);

        assertThat(user.getName()).isEqualTo("New");
        assertThat(user.getSurname()).isEqualTo("Name");
        assertThat(user.getEmail()).isEqualTo("new@test.com");
    }
}