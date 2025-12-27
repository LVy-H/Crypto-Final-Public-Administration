package com.gov.crypto.identityservice.service;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Identity Service - Authentication operations.
 * Tests match the actual AuthService API.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("citizen01");
        testUser.setEmail("citizen01@gov.vn");
        testUser.setPasswordHash("rawPassword");
        testUser.setRole("CITIZEN");
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
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate token for username")
        void shouldGenerateTokenForUsername() {
            // Given
            String expectedToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            when(userRepository.findByUsername("citizen01")).thenReturn(java.util.Optional.of(testUser));
            when(jwtService.generateToken("citizen01", "USER", "UNVERIFIED")).thenReturn(expectedToken);

            // When
            String token = authService.generateToken("citizen01");

            // Then
            assertEquals(expectedToken, token);
            verify(jwtService).generateToken("citizen01", "USER", "UNVERIFIED");
        }

        @Test
        @DisplayName("Should use default USER role for token generation")
        void shouldUseDefaultUserRole() {
            // Given
            User defaultUser = new User();
            defaultUser.setUsername("anyuser");
            // No role set, no status set

            when(userRepository.findByUsername("anyuser")).thenReturn(java.util.Optional.of(defaultUser));
            when(jwtService.generateToken(anyString(), eq("USER"), eq("UNVERIFIED"))).thenReturn("token");

            // When
            authService.generateToken("anyuser");

            // Then
            verify(jwtService).generateToken("anyuser", "USER", "UNVERIFIED");
        }
    }
}
