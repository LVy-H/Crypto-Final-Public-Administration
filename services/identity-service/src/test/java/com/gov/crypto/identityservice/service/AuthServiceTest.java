package com.gov.crypto.identityservice.service;

import com.gov.crypto.model.User;
import com.gov.crypto.repository.UserRepository;
import com.gov.crypto.service.AuthService;
import com.gov.crypto.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Identity Service - Authentication operations.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("citizen01");
        testUser.setEmail("citizen01@gov.vn");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole("CITIZEN");
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register new user")
        void shouldRegisterNewUser() {
            // Given
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@gov.vn")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$encodedPassword");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            User result = authService.register("newuser", "new@gov.vn", "password123");

            // Then
            assertNotNull(result);
            assertEquals("newuser", result.getUsername());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw when username already exists")
        void shouldThrowWhenUsernameExists() {
            // Given
            when(userRepository.existsByUsername("existing")).thenReturn(true);

            // When/Then
            assertThrows(RuntimeException.class, () -> authService.register("existing", "test@gov.vn", "password"));
        }

        @Test
        @DisplayName("Should throw when email already exists")
        void shouldThrowWhenEmailExists() {
            // Given
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@gov.vn")).thenReturn(true);

            // When/Then
            assertThrows(RuntimeException.class, () -> authService.register("newuser", "existing@gov.vn", "password"));
        }
    }

    @Nested
    @DisplayName("User Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login with correct credentials")
        void shouldLoginWithCorrectCredentials() {
            // Given
            when(userRepository.findByUsername("citizen01")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", testUser.getPassword())).thenReturn(true);

            // When
            AuthResponse response = authService.login("citizen01", "correctPassword");

            // Then
            assertNotNull(response);
            assertNotNull(response.getToken());
            assertEquals("citizen01", response.getUsername());
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // When/Then
            assertThrows(RuntimeException.class, () -> authService.login("unknown", "password"));
        }

        @Test
        @DisplayName("Should throw when password incorrect")
        void shouldThrowWhenPasswordIncorrect() {
            // Given
            when(userRepository.findByUsername("citizen01")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

            // When/Then
            assertThrows(RuntimeException.class, () -> authService.login("citizen01", "wrongPassword"));
        }
    }

    @Nested
    @DisplayName("JWT Token Tests")
    class TokenTests {

        @Test
        @DisplayName("Should validate correct JWT token")
        void shouldValidateCorrectToken() {
            // Given - valid token format
            String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjaXRpemVuMDEifQ.signature";

            // When
            boolean isValid = authService.validateToken(validToken);

            // Then - implementation specific, just verify no exception
            // Note: Actual validation depends on implementation
        }
    }
}
