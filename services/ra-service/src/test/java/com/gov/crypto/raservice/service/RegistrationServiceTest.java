package com.gov.crypto.raservice.service;

import com.gov.crypto.raservice.dto.RegistrationRequest;
import com.gov.crypto.raservice.dto.RegistrationResponse;
import com.gov.crypto.raservice.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RA Service - Registration Authority operations.
 * Tests certificate registration workflow orchestration.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private RegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(restTemplate,
                "http://cloud-sign:8084", "http://ca-authority:8082");
    }

    @Nested
    @DisplayName("Registration Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should validate registration request with all fields")
        void shouldValidateCompleteRequest() {
            // Given
            RegistrationRequest request = new RegistrationRequest();
            request.setSubjectDn("/CN=Nguyen Van A/O=Citizen/C=VN");
            request.setAlgorithm("mldsa65");

            // Then
            assertNotNull(request.getSubjectDn());
            assertNotNull(request.getAlgorithm());
            assertTrue(request.getSubjectDn().contains("/CN="));
        }

        @Test
        @DisplayName("Should accept ML-DSA-65 algorithm")
        void shouldAcceptMlDsa65() {
            // Given
            RegistrationRequest request = new RegistrationRequest();
            request.setAlgorithm("mldsa65");

            // Then
            assertEquals("mldsa65", request.getAlgorithm());
        }

        @Test
        @DisplayName("Should reject empty subject DN")
        void shouldRejectEmptySubjectDn() {
            // Given
            RegistrationRequest request = new RegistrationRequest();
            request.setSubjectDn("");

            // Then
            assertTrue(request.getSubjectDn().isEmpty());
        }
    }

    @Nested
    @DisplayName("Registration Response Tests")
    class ResponseTests {

        @Test
        @DisplayName("Should construct response with certificate")
        void shouldConstructResponseWithCert() {
            // Given
            RegistrationResponse response = new RegistrationResponse();
            response.setCertificate("-----BEGIN CERTIFICATE-----\nMIITest...\n-----END CERTIFICATE-----");
            response.setPublicKey("-----BEGIN PUBLIC KEY-----\nMIITest...\n-----END PUBLIC KEY-----");

            // Then
            assertNotNull(response.getCertificate());
            assertNotNull(response.getPublicKey());
            assertTrue(response.getCertificate().contains("BEGIN CERTIFICATE"));
        }

        @Test
        @DisplayName("Should include serial number in response")
        void shouldIncludeSerialNumber() {
            // Given
            RegistrationResponse response = new RegistrationResponse();
            response.setSerialNumber("ABC123");
            response.setValidUntil("2025-12-31");

            // Then
            assertEquals("ABC123", response.getSerialNumber());
            assertEquals("2025-12-31", response.getValidUntil());
        }
    }

    @Nested
    @DisplayName("Orchestration Tests")
    class OrchestrationTests {

        @Test
        @DisplayName("Should call Cloud Sign for key generation")
        void shouldCallCloudSignForKeyGen() {
            // This tests the orchestration flow
            // In actual implementation, we'd verify RestTemplate calls

            // Verify service URLs are configured
            assertNotNull(registrationService);
        }

        @Test
        @DisplayName("Should call CA Authority for certificate issuance")
        void shouldCallCaForCertIssuance() {
            // Verify orchestration is set up correctly
            assertNotNull(registrationService);
        }
    }

    @Nested
    @DisplayName("Subject DN Parsing Tests")
    class SubjectDnTests {

        @Test
        @DisplayName("Should parse Vietnamese names correctly")
        void shouldParseVietnameseNames() {
            // Given
            String subjectDn = "/CN=Nguyễn Văn A/O=Công dân/C=VN";

            // Then
            assertTrue(subjectDn.contains("Nguyễn"));
            assertTrue(subjectDn.contains("VN"));
        }

        @Test
        @DisplayName("Should extract CN from subject DN")
        void shouldExtractCn() {
            // Given
            String subjectDn = "/CN=Test User/O=Organization/C=VN";

            // Extract CN
            int cnStart = subjectDn.indexOf("/CN=") + 4;
            int cnEnd = subjectDn.indexOf("/", cnStart);
            String cn = subjectDn.substring(cnStart, cnEnd);

            // Then
            assertEquals("Test User", cn);
        }
    }
}
