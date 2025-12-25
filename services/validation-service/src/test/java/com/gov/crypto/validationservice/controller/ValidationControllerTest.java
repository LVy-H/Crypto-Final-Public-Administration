package com.gov.crypto.validationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 */
@WebMvcTest(ValidationController.class)
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ValidationService validationService;

    @Nested
    @DisplayName("POST /api/v1/validation/verify - Verify Signature")
    class VerifySignatureTests {

        @Test
        @DisplayName("Should verify valid signature successfully")
        void shouldVerifyValidSignature() throws Exception {
            // Given
            VerifyResponse mockResponse = new VerifyResponse();
            mockResponse.setValid(true);
            mockResponse.setMessage("Signature verified successfully");
            when(validationService.verify(any())).thenReturn(mockResponse);

            String requestBody = objectMapper.writeValueAsString(new VerifyRequest());

            // When/Then
            mockMvc.perform(post("/api/v1/validation/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"data\":\"dGVzdA==\",\"signature\":\"c2ln\",\"publicKey\":\"key\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("Should return invalid for bad signature")
        void shouldReturnInvalidForBadSignature() throws Exception {
            // Given
            VerifyResponse mockResponse = new VerifyResponse();
            mockResponse.setValid(false);
            mockResponse.setMessage("Signature verification failed");
            when(validationService.verify(any())).thenReturn(mockResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/validation/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"data\":\"dGVzdA==\",\"signature\":\"YmFk\",\"publicKey\":\"key\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }

        @Test
        @DisplayName("Should return 400 for missing data")
        void shouldReturn400ForMissingData() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/validation/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/validation/verify-chain - Verify Certificate Chain")
    class VerifyCertChainTests {

        @Test
        @DisplayName("Should verify complete certificate chain")
        void shouldVerifyCompleteChain() throws Exception {
            // Given
            VerifyResponse mockResponse = new VerifyResponse();
            mockResponse.setValid(true);
            when(validationService.verifyCertificateChain(any())).thenReturn(mockResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/validation/verify-chain")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"certificates\":[\"cert1\",\"cert2\",\"rootCert\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }
    }
}
