package com.gov.crypto.validationservice.controller;

import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.ValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for Validation Service - signature verification endpoints.
 * Tests match the actual ValidationController API with record-based DTOs.
 */
@WebMvcTest(ValidationController.class)
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ValidationService validationService;

    @Nested
    @DisplayName("POST /api/v1/validation/verify - Verify Signature")
    class VerifySignatureTests {

        @Test
        @DisplayName("Should verify valid signature successfully")
        void shouldVerifyValidSignature() throws Exception {
            // Given
            VerifyResponse mockResponse = new VerifyResponse(true, "Signature verified successfully");
            when(validationService.verifySignature(any(VerifyRequest.class))).thenReturn(mockResponse);

            String requestBody = """
                    {
                        "originalDocHash": "dGVzdA==",
                        "signatureBase64": "c2ln",
                        "certPem": "-----BEGIN CERTIFICATE-----\\ntest\\n-----END CERTIFICATE-----"
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/api/v1/validation/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isValid").value(true))
                    .andExpect(jsonPath("$.details").value("Signature verified successfully"));
        }

        @Test
        @DisplayName("Should return invalid for bad signature")
        void shouldReturnInvalidForBadSignature() throws Exception {
            // Given
            VerifyResponse mockResponse = new VerifyResponse(false, "Signature verification failed");
            when(validationService.verifySignature(any(VerifyRequest.class))).thenReturn(mockResponse);

            String requestBody = """
                    {
                        "originalDocHash": "dGVzdA==",
                        "signatureBase64": "YmFk",
                        "certPem": "-----BEGIN CERTIFICATE-----\\ntest\\n-----END CERTIFICATE-----"
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/api/v1/validation/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isValid").value(false));
        }

        @Test
        @DisplayName("Should call verifySignature service method")
        void shouldCallVerifySignatureService() throws Exception {
            // Given
            VerifyResponse mockResponse = new VerifyResponse(true, "OK");
            when(validationService.verifySignature(any(VerifyRequest.class))).thenReturn(mockResponse);

            String requestBody = """
                    {
                        "originalDocHash": "aGFzaA==",
                        "signatureBase64": "c2ln",
                        "certPem": "-----BEGIN CERTIFICATE-----\\ntest\\n-----END CERTIFICATE-----"
                    }
                    """;

            // When
            mockMvc.perform(post("/api/v1/validation/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());

            // Then
            verify(validationService).verifySignature(any(VerifyRequest.class));
        }
    }

    @Nested
    @DisplayName("Request/Response DTO Tests")
    class DtoTests {

        @Test
        @DisplayName("Should handle valid Base64 in request")
        void shouldHandleBase64InRequest() throws Exception {
            // Given
            String base64Data = Base64.getEncoder().encodeToString("test-data".getBytes());
            VerifyResponse mockResponse = new VerifyResponse(true, "Valid");
            when(validationService.verifySignature(any())).thenReturn(mockResponse);

            String requestBody = """
                    {
                        "originalDocHash": "%s",
                        "signatureBase64": "c2ln",
                        "certPem": "-----BEGIN CERTIFICATE-----\\ntest\\n-----END CERTIFICATE-----"
                    }
                    """.formatted(base64Data);

            // When/Then
            mockMvc.perform(post("/api/v1/validation/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());
        }
    }
}
