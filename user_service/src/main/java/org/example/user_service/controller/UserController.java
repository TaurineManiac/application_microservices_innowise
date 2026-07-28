package org.example.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user_service.dto.CreateUserRequest;
import org.example.user_service.dto.UpdateUserRequest;
import org.example.user_service.dto.UserResponse;
import org.example.user_service.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("REST request to create user: {}", request.getEmail());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @GetMapping("/{publicId}")
    public ResponseEntity<UserResponse> getUserByPublicId(@PathVariable UUID publicId) {
        log.info("REST request to get user by publicId: {}", publicId);
        UserResponse response = userService.getUserResponseByPublicId(publicId);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST request to get all users with filters - name: {}, surname: {}, email: {}, active: {}",
                name, surname, email, active);

        Page<UserResponse> page = userService.getAllUsers(name, surname, email, active, pageable);
        return ResponseEntity.ok(page);
    }



    @PutMapping("/{publicId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID publicId,
            @Valid @RequestBody UpdateUserRequest request) {

        log.info("REST request to update user with publicId: {}", publicId);
        UserResponse response = userService.updateUser(publicId, request);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{publicId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable UUID publicId) {
        log.info("REST request to activate user with publicId: {}", publicId);
        userService.activateUser(publicId);
        return ResponseEntity.noContent().build();
    }



    @PatchMapping("/{publicId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID publicId) {
        log.info("REST request to deactivate user with publicId: {}", publicId);
        userService.deactivateUser(publicId);
        return ResponseEntity.noContent().build();
    }
}