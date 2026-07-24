package org.example.user_service.unit;

import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.UpdateUserRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.entity.User;
import org.example.user_service.exception.EmailAlreadyExistsException;
import org.example.user_service.exception.EntityNotFoundException;
import org.example.user_service.mapper.UserMapper;
import org.example.user_service.repository.UserRepository;
import org.example.user_service.service.PaymentCardService;
import org.example.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardService paymentCardService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .publicId("public-id-123")
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();

        createRequest = CreateUserRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        updateRequest = UpdateUserRequest.builder()
                .name("Jonathan")
                .surname("Doe")
                .email("jonathan@example.com")
                .build();

        userResponse = UserResponse.builder()
                .publicId("public-id-123")
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .active(true)
                .build();
    }

    @Test
    void createUser_shouldSaveAndReturnUser() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(CreateUserRequest.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UserResponse result = userService.createUser(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getPublicId()).isEqualTo("public-id-123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowException_whenEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserResponseByPublicId_shouldReturnCachedUser() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UserResponse result = userService.getUserResponseByPublicId("public-id-123");

        assertThat(result).isNotNull();
        assertThat(result.getPublicId()).isEqualTo("public-id-123");
        verify(userRepository).findByPublicId("public-id-123");
    }

    @Test
    void getUserResponseByPublicId_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserResponseByPublicId("invalid"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAllUsers_shouldReturnPage() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page.map(u -> u));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        var result = userService.getAllUsers("John", null, null, true, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void updateUser_shouldUpdateAndEvictCache() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UserResponse result = userService.updateUser("public-id-123", updateRequest);


        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(paymentCardService).updateCardsHolderForUser(eq(1L), eq("public-id-123"), anyString());
    }

    @Test
    void updateUser_shouldThrow_whenEmailExists() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser("public-id-123", updateRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void activateUser_shouldSetActiveTrue() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.activateUser("public-id-123");

        assertThat(user.getActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_shouldSetActiveFalse() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deactivateUser("public-id-123");

        assertThat(user.getActive()).isFalse();
        verify(userRepository).save(user);
    }
}