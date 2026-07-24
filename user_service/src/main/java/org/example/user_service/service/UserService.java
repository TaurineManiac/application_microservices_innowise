package org.example.user_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user_service.constant.AppConstraint;
import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.UpdateUserRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.entity.User;
import org.example.user_service.exception.EmailAlreadyExistsException;
import org.example.user_service.exception.EntityNotFoundException;
import org.example.user_service.mapper.UserMapper;
import org.example.user_service.repository.UserRepository;
import org.example.user_service.specification.UserSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    UserRepository userRepository;
    PaymentCardService paymentCardService;
    UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest createUserRequest) {
        if(userRepository.existsByEmail(createUserRequest.getEmail())){
            throw new EmailAlreadyExistsException("Email already exists");
        }
        User user = userMapper.toEntity(createUserRequest);
        user.setActive(true);

        String uuid = UUID.randomUUID().toString();
        AtomicInteger counter = new AtomicInteger();
        while (userRepository.existsByPublicId(uuid)){
            uuid = UUID.randomUUID().toString();
            counter.incrementAndGet();
            if(counter.get() >= AppConstraint.MAX_RETRIES_FOR_PUBLIC_ID.getValue()){
                throw new RuntimeException("Something went wrong");
            }
        }

        user.setPublicId(uuid);
        User savedUser = userRepository.save(user);

        log.info("User created successfully with publicId: {}", savedUser.getPublicId());
        return userMapper.toUserResponse(savedUser);
    }

    @Cacheable(value = "users", key = "#publicId")
    @Transactional(readOnly = true)
    public UserResponse getUserResponseByPublicId(String publicId) {
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
    public UserResponse updateUser(String publicId, UpdateUserRequest request) {
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

// I prefer not to use it, because in real bank application we won't delete sensitive data, I suppose.
//    @Transactional
//    public void deleteUser(String publicId) {
//        User user = userRepository.findByPublicId(publicId)
//                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));
//        userRepository.delete(user);
//    }

    @CacheEvict(value = "users", key = "#publicId")
    @Transactional
    public void deactivateUser(String publicId) {
        log.info("Deactivating user with publicId: {}", publicId);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated successfully: {}", publicId);
    }



    @CacheEvict(value = "users", key = "#publicId")
    @Transactional
    public void activateUser(String publicId) {
        log.info("Activating user with publicId: {}", publicId);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));
        user.setActive(true);
        userRepository.save(user);
        log.info("User activated successfully: {}", publicId);
    }


    @Transactional(readOnly = true)
    public User getUserEntityByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicId));
    }


}
