package org.example.user_service.unit;

import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.UpdateUserRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.dto.UserStatusEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private KafkaTemplate<String, UserStatusEvent> kafkaTemplate;

    @Mock
    private PaymentCardService paymentCardService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .name("John")
                .surname("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@test.com")
                .active(true)
                .build();

        createRequest = CreateUserRequest.builder()
                .name("John")
                .surname("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@test.com")
                .build();

        updateRequest = UpdateUserRequest.builder()
                .name("Jonathan")
                .surname("Smith")
                .build();
    }

    @Test
    void createUser_shouldSaveAndReturnUser_whenEmailIsUnique() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toEntity(createRequest)).thenReturn(user);
        when(userMapper.toUserResponse(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return UserResponse.builder()
                    .publicId(u.getPublicId())
                    .email(u.getEmail())
                    .build();
        });

        UserResponse response = userService.createUser(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getPublicId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        verify(userRepository).save(any(User.class));
        verify(userRepository, never()).existsByPublicId(any(UUID.class));
    }

    @Test
    void createUser_shouldThrowEmailAlreadyExistsException_whenEmailTaken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void createUser_shouldRetryOnUuidCollision() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(createRequest)).thenReturn(user);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate UUID"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return UserResponse.builder().publicId(u.getPublicId()).build();
        });

        UserResponse response = userService.createUser(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getPublicId()).isNotNull();
        verify(userRepository, times(2)).save(any(User.class));
        verify(userRepository, never()).existsByPublicId(any(UUID.class));
    }

    @Test
    void getUserByPublicId_shouldReturnUser_whenExists() {
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(any(User.class)))
                .thenReturn(UserResponse.builder().publicId(user.getPublicId()).build());

        UserResponse response = userService.getUserResponseByPublicId(user.getPublicId());

        assertThat(response.getPublicId()).isEqualTo(user.getPublicId());
    }

    @Test
    void getUserByPublicId_shouldThrowEntityNotFoundException_whenNotFound() {
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserResponseByPublicId(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateUser_shouldUpdateAndEvictCache_whenNameChanges() {
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            UpdateUserRequest req = invocation.getArgument(0);
            User u = invocation.getArgument(1);
            if (req.getName() != null) u.setName(req.getName());
            if (req.getSurname() != null) u.setSurname(req.getSurname());
            return null;
        }).when(userMapper).updateEntity(any(UpdateUserRequest.class), any(User.class));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(UserResponse.builder().publicId(user.getPublicId()).build());

        userService.updateUser(user.getPublicId(), updateRequest);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(paymentCardService).updateCardsHolderForUser(eq(1L), eq(user.getPublicId()), nameCaptor.capture());
        assertThat(nameCaptor.getValue()).isEqualTo("Jonathan Smith");
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_shouldSetActiveFalse() {
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        userService.deactivateUser(user.getPublicId());
        assertThat(user.getActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void getAllUsers_shouldReturnPageWithFilters() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page.map(u -> u));
        when(userMapper.toUserResponse(any(User.class)))
                .thenReturn(UserResponse.builder().publicId(user.getPublicId()).build());

        Page<UserResponse> result = userService.getAllUsers("John", null, null, true, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }
}