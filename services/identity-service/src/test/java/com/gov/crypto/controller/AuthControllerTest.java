package com.gov.crypto.controller;

import com.gov.crypto.identityservice.service.AuthService;
import com.gov.crypto.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for Identity Service authentication endpoints.
 * Tests match the actual AuthController API.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security for unit tests
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Nested
    @DisplayName("POST /api/v1/auth/register - User Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUser() throws Exception {
            // Given
            when(authService.saveUser(any(User.class))).thenReturn("User added to system");

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"newuser\",\"email\":\"new@gov.vn\",\"password\":\"Pass123!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User added to system"))
                    .andExpect(jsonPath("$.username").value("newuser"));
        }

        @Test
        @DisplayName("Should call saveUser with correct user data")
        void shouldCallSaveUserCorrectly() throws Exception {
            // Given
            when(authService.saveUser(any(User.class))).thenReturn("User added to system");

            // When
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"testuser\",\"email\":\"test@gov.vn\",\"password\":\"TestPass!\"}"))
                    .andExpect(status().isOk());

            // Then
            verify(authService).saveUser(any(User.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login - User Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            // Given - mock authentication and token generation
            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.isAuthenticated()).thenReturn(true);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(authService.generateToken("testuser")).thenReturn("jwt.token.here");

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"testuser\",\"password\":\"Pass123!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt.token.here"))
                    .andExpect(jsonPath("$.user.username").value("testuser"));
        }

        @Test
        @DisplayName("Should generate token on successful authentication")
        void shouldGenerateTokenOnSuccess() throws Exception {
            // Given
            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.isAuthenticated()).thenReturn(true);
            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(authService.generateToken(anyString())).thenReturn("test.jwt.token");

            // When
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"user\",\"password\":\"pass\"}"))
                    .andExpect(status().isOk());

            // Then
            verify(authService).generateToken("user");
        }
    }
}
