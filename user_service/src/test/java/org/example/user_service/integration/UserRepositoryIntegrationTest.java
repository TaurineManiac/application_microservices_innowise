package org.example.user_service.integration;

import org.example.user_service.entity.User;
import org.example.user_service.repository.UserRepository;
import org.example.user_service.specification.UserSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Testcontainers
public class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindUser() {
        User user = User.builder()
                .publicId("pub-123")
                .name("John")
                .surname("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@test.com")
                .active(true)
                .build();

        userRepository.save(user);

        User found = userRepository.findByPublicId("pub-123").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("John");
    }

    @Test
    void shouldFindByEmail() {
        User user = User.builder()
                .publicId("pub-456")
                .name("Jane")
                .surname("Smith")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .email("jane@test.com")
                .active(true)
                .build();

        userRepository.save(user);

        User found = userRepository.getUserByEmail("jane@test.com").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getSurname()).isEqualTo("Smith");
    }

    @Test
    void shouldFindBySpecification() {
        User user1 = User.builder()
                .publicId("pub-1")
                .name("Alice")
                .surname("Brown")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .email("alice@test.com")
                .active(true)
                .build();

        User user2 = User.builder()
                .publicId("pub-2")
                .name("Bob")
                .surname("Brown")
                .dateOfBirth(LocalDate.of(1988, 8, 8))
                .email("bob@test.com")
                .active(false)
                .build();

        userRepository.saveAll(List.of(user1, user2));

        Specification<User> spec = Specification
                .where(UserSpecification.hasSurname("Brown"))
                .and(UserSpecification.isActive(true));

        List<User> result = userRepository.findAll(spec);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }
}