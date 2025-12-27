package com.gov.crypto.caauthority.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.service.HierarchicalCaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API/Controller tests for CA Authority endpoints.
 * Tests HTTP layer, request/response handling, and error responses.
 */
@WebMvcTest(HierarchicalCaController.class)
class HierarchicalCaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HierarchicalCaService caService;

    private CertificateAuthority rootCa;
    private CertificateAuthority provincialCa;

    @BeforeEach
    void setUp() {
        rootCa = createTestCa("National Root CA", CaType.ISSUING_CA, 0, "ML-DSA-87", null);
        provincialCa = createTestCa("Ho Chi Minh City", CaType.ISSUING_CA, 1, "ML-DSA-87", rootCa);
    }

    private CertificateAuthority createTestCa(String name, CaType type, int level, String algorithm,
            CertificateAuthority parent) {
        CertificateAuthority ca = new CertificateAuthority();
        ca.setId(UUID.randomUUID());
        ca.setName(name);
        ca.setType(type);
        ca.setHierarchyLevel(level);
        ca.setAlgorithm(algorithm);
        ca.setParentCa(parent);
        ca.setStatus(CaStatus.ACTIVE);
        ca.setValidFrom(LocalDateTime.now());
        ca.setValidUntil(LocalDateTime.now().plusYears(10));
        return ca;
    }

    @Nested
    @DisplayName("POST /api/v1/ca/root/init - Initialize Root CA")
    class InitRootCaTests {

        @Test
        @DisplayName("Should return 200 and root CA details on success")
        void shouldInitRootCaSuccessfully() throws Exception {
            // Given
            when(caService.initializeRootCa(any())).thenReturn(rootCa);

            // When/Then
            mockMvc.perform(post("/api/v1/ca/root/init")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"National PQC Root CA\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("National Root CA"))
                    .andExpect(jsonPath("$.algorithm").value("ML-DSA-87"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 500 on service exception")
        void shouldReturn500OnException() throws Exception {
            // Given
            when(caService.initializeRootCa(any())).thenThrow(new RuntimeException("OpenSSL not found"));

            // When/Then
            mockMvc.perform(post("/api/v1/ca/root/init")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Test CA\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ca/provincial - Create Provincial CA")
    class CreateProvincialCaTests {

        @Test
        @DisplayName("Should create provincial CA successfully")
        void shouldCreateProvincialCa() throws Exception {
            // Given
            when(caService.createProvincialCa(any(), any())).thenReturn(provincialCa);

            // When/Then
            mockMvc.perform(post("/api/v1/ca/provincial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "parentCaId", rootCa.getId().toString(),
                            "name", "Ho Chi Minh City"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ho Chi Minh City"))
                    .andExpect(jsonPath("$.algorithm").value("ML-DSA-87"));
        }

        @Test
        @DisplayName("Should return 500 for invalid parent CA")
        void shouldReturn500ForInvalidParent() throws Exception {
            // Given
            when(caService.createProvincialCa(any(), any()))
                    .thenThrow(new RuntimeException("Parent CA not found"));

            // When/Then
            mockMvc.perform(post("/api/v1/ca/provincial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"parentCaId\": \"" + UUID.randomUUID() + "\", \"name\": \"Test\"}"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ca/level/{level} - Get CAs by Level")
    class GetCasByLevelTests {

        @Test
        @DisplayName("Should return list of ROOT CAs")
        void shouldReturnRootCas() throws Exception {
            // Given
            when(caService.getCasByLevel(0)).thenReturn(List.of(rootCa));

            // When/Then
            mockMvc.perform(get("/api/v1/ca/level/0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("National Root CA"));
        }

        @Test
        @DisplayName("Should return 400 for invalid level")
        void shouldReturn400ForInvalidLevel() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/ca/level/INVALID"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ca/chain/{caId} - Get Certificate Chain")
    class GetCertChainTests {

        @Test
        @DisplayName("Should return certificate chain")
        void shouldReturnCertChain() throws Exception {
            // Given
            when(caService.getCertificateChain(any())).thenReturn(List.of("cert1", "cert2", "cert3"));

            // When/Then
            mockMvc.perform(get("/api/v1/ca/chain/" + UUID.randomUUID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ca/revoke-ca/{caId} - Revoke CA")
    class RevokeCaTests {

        @Test
        @DisplayName("Should revoke CA successfully")
        void shouldRevokeCa() throws Exception {
            // Given
            doNothing().when(caService).revokeCa(any(), any());

            // When/Then
            mockMvc.perform(post("/api/v1/ca/revoke-ca/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\": \"Security breach\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("revoked"));
        }
    }
}
