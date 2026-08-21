package org.example.user_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user_service.constant.AppConstraint;
import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.UpdateUserRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.dto.UserStatusEvent;
import org.example.user_service.entity.User;
import org.example.user_service.exception.EmailAlreadyExistsException;
import org.example.user_service.exception.EntityNotFoundException;
import org.example.user_service.exception.GenerationException;
import org.example.user_service.mapper.UserMapper;
import org.example.user_service.repository.UserRepository;
import org.example.user_service.specification.UserSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${internal.service.token}")
    private String internalToken;
    private final UserRepository userRepository;
    private final PaymentCardService paymentCardService;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, UserStatusEvent> kafkaTemplate;
    private static final String USER_STATUS_TOPIC = "user-status-events";

    @Transactional
    public UserResponse createUser(CreateUserRequest createUserRequest) {
        if(userRepository.existsByEmail(createUserRequest.getEmail())){
            throw new EmailAlreadyExistsException("Email already exists");
        }
        User user = userMapper.toEntity(createUserRequest);
        user.setActive(true);

        for(int i =0; i < AppConstraint.MAX_RETRIES_FOR_PUBLIC_ID.getValue(); i++){
            try{
                user.setPublicId(UUID.randomUUID());

                User savedUser = userRepository.save(user);

                log.info("User created successfully with publicId: {} (attempt {})",
                        savedUser.getPublicId(), i+1);
                return userMapper.toUserResponse(savedUser);
            }
            catch (DataIntegrityViolationException ex){
                log.warn("UUID collision on attempt {}/{}, retrying...", i, AppConstraint.MAX_RETRIES_FOR_PUBLIC_ID.getValue());

                if (i == AppConstraint.MAX_RETRIES_FOR_PUBLIC_ID.getValue()-1) {
                    log.error("Failed to generate unique UUID after {} attempts", AppConstraint.MAX_RETRIES_FOR_PUBLIC_ID.getValue());
                    throw new GenerationException("Something went wrong, retry again later.");
                }
            }
        }
        throw new GenerationException("Something went wrong, retry again later.");
    }

    @Transactional
    @CacheEvict
    public void rollbackUser(UUID publicId, String providedToken) {
        if(providedToken == null || providedToken.isEmpty() || !providedToken.equals(internalToken)){
            log.warn("Invalid internal token for rollback attempt on user: {}", publicId);
            throw new SecurityException("Invalid internal token");
        }

        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + publicId));
        userRepository.delete(user);
        log.warn("User {} hard-deleted due to rollback", publicId);

    }

    @Cacheable(value = "users", key = "#publicId")
    @Transactional(readOnly = true)
    public UserResponse getUserResponseByPublicId(UUID publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(
            String name,
            String surname,
            String email,
            Boolean active,
            Pageable pageable) {

        log.debug("Fetching all users with filters - name: {}, surname: {}, email: {}, active: {}",
                name, surname, email, active);

        Specification<User> spec = Specification
                .where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname))
                .and(UserSpecification.hasEmail(email))
                .and(UserSpecification.isActive(active));

        return userRepository.findAll(spec, pageable)
                .map(userMapper::toUserResponse);
    }

    @CacheEvict(value = "users", key = "#publicId")
    @Transactional
    public UserResponse updateUser(UUID publicId, UpdateUserRequest request) {
        log.info("Updating user with publicId: {}", publicId);

        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
            }
        }

        String oldName = user.getName();
        String oldSurname = user.getSurname();

        userMapper.updateEntity(request, user);

        boolean nameChanged = (request.getName() != null && !request.getName().equals(oldName)) ||
                (request.getSurname() != null && !request.getSurname().equals(oldSurname));

        if (nameChanged) {
            String newFullName = user.getName() + " " + user.getSurname();
            log.info("User name changed. Propagating new holder name '{}' to all cards.", newFullName);
            paymentCardService.updateCardsHolderForUser(user.getId(), user.getPublicId(), newFullName);
        }

        User updated = userRepository.save(user);
        log.info("User updated successfully: {}", updated.getPublicId());
        return userMapper.toUserResponse(updated);
    }


    @CacheEvict(value = "users", key = "#publicId")
    @Transactional
    public void deactivateUser(UUID publicId) {

        log.info("Deactivating user with publicId: {}", publicId);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));
        user.setActive(false);
        userRepository.save(user);

        UserStatusEvent event = UserStatusEvent.builder()
                .publicId(publicId)
                .active(false)
                .build();
        kafkaTemplate.send(USER_STATUS_TOPIC, event);
        log.info("User deactivated successfully: {}", publicId);
    }



    @CacheEvict(value = "users", key = "#publicId")
    @Transactional
    public void activateUser(UUID publicId) {
        log.info("Activating user with publicId: {}", publicId);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));
        user.setActive(true);
        userRepository.save(user);

        UserStatusEvent event = UserStatusEvent.builder()
                .publicId(publicId)
                .active(true)
                .build();
        kafkaTemplate.send(USER_STATUS_TOPIC, event);
        log.info("User activated successfully: {}", publicId);
    }


    @Transactional(readOnly = true)
    public User getUserEntityByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));
    }


}
