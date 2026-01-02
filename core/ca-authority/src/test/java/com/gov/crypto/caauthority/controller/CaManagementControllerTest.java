package com.gov.crypto.caauthority.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.service.CaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

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
 * 
 * Uses strict isolation via @ContextConfiguration to avoid loading the main
 * app's broad ComponentScan.
 */
@WebMvcTest(controllers = CaManagementController.class)
@ContextConfiguration(classes = {
        CaManagementController.class,
        CaManagementControllerTest.TestConfig.class
})
@AutoConfigureMockMvc(addFilters = false)
class CaManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CaService caService;

    @TestConfiguration
    @EnableWebMvc
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private CertificateAuthority rootCa;
    private CertificateAuthority provincialCa;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.security.core.Authentication authentication;

    @BeforeEach
    void setUp() {
        rootCa = createTestCa("National Root CA", CaType.ISSUING_CA, 0, "ML-DSA-87", null);
        provincialCa = createTestCa("Ho Chi Minh City", CaType.ISSUING_CA, 1, "ML-DSA-87", rootCa);

        // Mock Security Context
        authentication = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = mock(
                org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
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

    @Nested
    @DisplayName("GET /api/v1/ca/crl/{caId} - Get CRL")
    class GetCrlTests {
        @Test
        @DisplayName("Should return CRL PEM")
        void shouldReturnCrl() throws Exception {
            // Given
            String crlPem = "-----BEGIN X509 CRL-----...-----END X509 CRL-----";
            when(caService.generateCrl(any())).thenReturn(crlPem);

            // When/Then
            mockMvc.perform(get("/api/v1/ca/crl/" + UUID.randomUUID()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(crlPem));
        }
    }

    @Nested
    @DisplayName("CSR Workflow Tests")
    class CsrWorkflowTests {

        @Test
        @DisplayName("POST /api/v1/ca/{parentId}/request - Should submit request")
        void shouldSubmitRequest() throws Exception {
            // Given
            UUID parentId = rootCa.getId();
            UUID pendingId = UUID.randomUUID();
            CaService.CsrResult result = new CaService.CsrResult(pendingId.toString(), "csr-pem");

            when(caService.submitCaRequest(eq(parentId), eq("New Sub CA"), eq("mldsa65"), any()))
                    .thenReturn(result);

            // When/Then
            mockMvc.perform(post("/api/v1/ca/" + parentId + "/request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"New Sub CA\", \"algorithm\": \"mldsa65\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingRequestId").value(pendingId.toString()))
                    .andExpect(jsonPath("$.csrPem").value("csr-pem"));
        }

        @Test
        @DisplayName("GET /api/v1/ca/requests/pending - Should list pending requests")
        void shouldListPendingRequests() throws Exception {
            // Given
            com.gov.crypto.caauthority.model.CaPendingRequest req = new com.gov.crypto.caauthority.model.CaPendingRequest();
            req.setId(UUID.randomUUID());
            req.setName("Pending CA");
            req.setAlgorithm("mldsa65");
            req.setRequestedBy("admin");
            req.setRequestedAt(LocalDateTime.now());
            req.setParentCa(rootCa);

            when(caService.getPendingCaRequests()).thenReturn(List.of(req));

            // When/Then
            mockMvc.perform(get("/api/v1/ca/requests/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("Pending CA"))
                    .andExpect(jsonPath("$[0].parentCaName").value("National Root CA"));
        }

        @Test
        @DisplayName("POST /api/v1/ca/requests/{requestId}/approve - Should approve request")
        void shouldApproveRequest() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            CertificateAuthority newCa = createTestCa("New Approved CA", CaType.ISSUING_CA, 1, "mldsa65", rootCa);

            when(caService.approveCaRequest(eq(requestId), any())).thenReturn(newCa);

            // When/Then
            mockMvc.perform(post("/api/v1/ca/requests/" + requestId + "/approve"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.name").value("New Approved CA"));
        }

        @Test
        @DisplayName("POST /api/v1/ca/requests/{requestId}/reject - Should reject request")
        void shouldRejectRequest() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            doNothing().when(caService).rejectCaRequest(eq(requestId), any(), any());

            // When/Then
            mockMvc.perform(post("/api/v1/ca/requests/" + requestId + "/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\": \"Bad name\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }
    }
}
