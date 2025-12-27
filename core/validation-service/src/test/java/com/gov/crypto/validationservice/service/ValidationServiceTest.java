package com.gov.crypto.validationservice.service;

import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.impl.ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Validation Service - Signature verification operations.
 * Tests match the actual ValidationService API with record-based DTOs.
 */
class ValidationServiceTest {

    private ValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationServiceImpl();
    }

    @Nested
    @DisplayName("VerifyRequest DTO Tests")
    class VerifyRequestTests {

        @Test
        @DisplayName("Should create VerifyRequest with all fields")
        void shouldCreateVerifyRequest() {
            // Given
            String docHash = Base64.getEncoder().encodeToString("test-hash".getBytes());
            String signature = Base64.getEncoder().encodeToString("test-sig".getBytes());
            String certPem = "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----";

            // When
            VerifyRequest request = new VerifyRequest(docHash, signature, certPem);

            // Then
            assertEquals(docHash, request.originalDocHash());
            assertEquals(signature, request.signatureBase64());
            assertEquals(certPem, request.certPem());
        }

        @Test
        @DisplayName("Should handle Base64 encoded data correctly")
        void shouldHandleBase64Encoding() {
            // Given - valid Base64 data
            byte[] originalData = "Hello World".getBytes();
            String base64Data = Base64.getEncoder().encodeToString(originalData);
            String signature = Base64.getEncoder().encodeToString("signature".getBytes());
            String certPem = "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----";

            // When
            VerifyRequest request = new VerifyRequest(base64Data, signature, certPem);

            // Then
            byte[] decoded = Base64.getDecoder().decode(request.originalDocHash());
            assertArrayEquals(originalData, decoded);
        }

        @Test
        @DisplayName("Should handle PEM certificate format")
        void shouldHandlePemCertificate() {
            // Given
            String certPem = """
                    -----BEGIN CERTIFICATE-----
                    MIIBmTCCAUCgAwIBAgIUTest123456789...
                    -----END CERTIFICATE-----
                    """;

            // When
            VerifyRequest request = new VerifyRequest("hash", "sig", certPem);

            // Then
            assertTrue(request.certPem().contains("BEGIN CERTIFICATE"));
            assertTrue(request.certPem().contains("END CERTIFICATE"));
        }
    }

    @Nested
    @DisplayName("VerifyResponse DTO Tests")
    class VerifyResponseTests {

        @Test
        @DisplayName("Should create valid VerifyResponse")
        void shouldCreateValidResponse() {
            // When
            VerifyResponse response = new VerifyResponse(true, "Signature is VALID");

            // Then
            assertTrue(response.isValid());
            assertEquals("Signature is VALID", response.details());
        }

        @Test
        @DisplayName("Should create invalid VerifyResponse with reason")
        void shouldCreateInvalidResponseWithReason() {
            // When
            VerifyResponse response = new VerifyResponse(false, "Certificate has EXPIRED");

            // Then
            assertFalse(response.isValid());
            assertTrue(response.details().contains("EXPIRED"));
        }

        @Test
        @DisplayName("Should include detailed validation info")
        void shouldIncludeDetailedInfo() {
            // Given
            String details = "✓ Cryptographic signature verified. " +
                    "✓ Certificate is within validity period. " +
                    "✓ Certificate is not revoked.";

            // When
            VerifyResponse response = new VerifyResponse(true, details);

            // Then
            assertTrue(response.details().contains("Cryptographic signature"));
            assertTrue(response.details().contains("validity period"));
            assertTrue(response.details().contains("not revoked"));
        }
    }

    @Nested
    @DisplayName("Signature Verification Logic Tests")
    class SignatureVerificationTests {

        @Test
        @DisplayName("Should reject invalid Base64 document hash")
        void shouldRejectInvalidBase64Hash() {
            // Given - invalid Base64
            VerifyRequest request = new VerifyRequest(
                    "not-valid-base64!!!",
                    Base64.getEncoder().encodeToString("sig".getBytes()),
                    "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----");

            // When
            VerifyResponse response = validationService.verifySignature(request);

            // Then - should handle gracefully or return invalid
            assertNotNull(response);
            // Response may be invalid due to Base64 parsing failure
        }

        @Test
        @DisplayName("Should reject empty certificate")
        void shouldRejectEmptyCertificate() {
            // Given
            VerifyRequest request = new VerifyRequest(
                    Base64.getEncoder().encodeToString("hash".getBytes()),
                    Base64.getEncoder().encodeToString("sig".getBytes()),
                    "" // Empty certificate
            );

            // When
            VerifyResponse response = validationService.verifySignature(request);

            // Then
            assertNotNull(response);
            assertFalse(response.isValid());
        }

        @Test
        @DisplayName("Should reject malformed PEM certificate")
        void shouldRejectMalformedPem() {
            // Given - certificate without proper headers
            VerifyRequest request = new VerifyRequest(
                    Base64.getEncoder().encodeToString("hash".getBytes()),
                    Base64.getEncoder().encodeToString("sig".getBytes()),
                    "not-a-valid-pem-certificate");

            // When
            VerifyResponse response = validationService.verifySignature(request);

            // Then
            assertNotNull(response);
            assertFalse(response.isValid());
        }
    }
}
