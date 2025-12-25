package com.gov.crypto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.crypto.dto.AuthRequest;
import com.gov.crypto.dto.AuthResponse;
import com.gov.crypto.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API/Controller tests for Identity Service authentication endpoints.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/v1/auth/register - User Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUser() throws Exception {
            // Given
            when(authService.register(any(), any(), any())).thenReturn(new com.gov.crypto.model.User());

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"newuser\",\"email\":\"new@gov.vn\",\"password\":\"Pass123!\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 for duplicate username")
        void shouldReturn400ForDuplicate() throws Exception {
            // Given
            when(authService.register(any(), any(), any()))
                    .thenThrow(new RuntimeException("Username already exists"));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"existing\",\"email\":\"test@gov.vn\",\"password\":\"Pass123!\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login - User Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            // Given
            AuthResponse mockResponse = new AuthResponse();
            mockResponse.setToken("jwt.token.here");
            mockResponse.setUsername("testuser");
            when(authService.login(any(), any())).thenReturn(mockResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"testuser\",\"password\":\"Pass123!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            // Given
            when(authService.login(any(), any()))
                    .thenThrow(new RuntimeException("Invalid credentials"));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"user\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me - Get Current User")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user with valid token")
        void shouldReturnCurrentUser() throws Exception {
            // Authorization header required
            mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer valid.jwt.token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 401 without token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
