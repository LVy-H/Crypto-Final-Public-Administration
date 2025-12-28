package com.gov.crypto.identityservice.service;

import com.gov.crypto.model.Role;
import com.gov.crypto.model.User;
import com.gov.crypto.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Identity Service - Authentication operations.
 * Tests session-based authentication.
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
    private Role citizenRole;

    @BeforeEach
    void setUp() {
        citizenRole = new Role();
        citizenRole.setName("CITIZEN");
        citizenRole.setOfficerRole(false);

        testUser = new User();
        testUser.setUsername("citizen01");
        testUser.setEmail("citizen01@gov.vn");
        testUser.setPasswordHash("rawPassword");
        testUser.setRole(citizenRole);
    }

    @Nested
    @DisplayName("User Save Tests")
    class SaveUserTests {

        @Test
        @DisplayName("Should successfully save user with encoded password")
        void shouldSaveUserWithEncodedPassword() {
            // Given
            when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            String result = authService.saveUser(testUser);

            // Then
            assertEquals("User added to system", result);
            verify(passwordEncoder).encode("rawPassword");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            // Given
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
            when(userRepository.save(any())).thenReturn(testUser);

            // When
            authService.saveUser(testUser);

            // Then
            verify(passwordEncoder).encode("rawPassword");
        }
    }

    @Nested
    @DisplayName("User Lookup Tests")
    class UserLookupTests {

        @Test
        @DisplayName("Should find user by username")
        void shouldFindUserByUsername() {
            // Given
            when(userRepository.findByUsername("citizen01")).thenReturn(java.util.Optional.of(testUser));

            // When
            var result = userRepository.findByUsername("citizen01");

            // Then
            assertTrue(result.isPresent());
            assertEquals("citizen01", result.get().getUsername());
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(java.util.Optional.empty());

            // When
            var result = userRepository.findByUsername("nonexistent");

            // Then
            assertTrue(result.isEmpty());
        }
    }
}
