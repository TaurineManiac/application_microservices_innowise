package org.example.user_service.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.user_service.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> getUserByEmail(@Email String email);

    @Query("SELECT u FROM User u where u.active = true")
    List<User> findAllActiveUsers();

    @Query(value = "SELECT * FROM users WHERE LOWER(name) = LOWER(:name) ",  nativeQuery = true)
    List<User> findAllByName(@Param("name") String name);

    @EntityGraph(attributePaths = {"paymentCards"})
    Optional<User> findById(Long id);

    @Query("SELECT COUNT(c) FROM PaymentCard c WHERE c.user.id = :userId")
    int countCardsByUserId(@Param("userId") Long userId);

    boolean existsByEmail(@NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email);

    @EntityGraph(attributePaths = {"paymentCards"})
    Optional<User> findByPublicId(UUID publicId);

    boolean existsByPublicId(UUID uuid);
}
