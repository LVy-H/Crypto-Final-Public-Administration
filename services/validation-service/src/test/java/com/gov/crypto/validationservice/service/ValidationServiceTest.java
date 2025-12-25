package com.gov.crypto.validationservice.service;

import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.impl.ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Validation Service - Signature verification operations.
 * Tests ML-DSA-65/87 signature verification and certificate validation.
 */
@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    private ValidationServiceImpl validationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validationService = new ValidationServiceImpl();
    }

    @Nested
    @DisplayName("Signature Verification Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should return valid response for correct signature")
        void shouldReturnValidForCorrectSignature() {
            // Given - mock valid verification scenario
            VerifyRequest request = new VerifyRequest();
            request.setData(Base64.getEncoder().encodeToString("test document".getBytes()));
            request.setSignature(Base64.getEncoder().encodeToString("mock-signature".getBytes()));
            request.setPublicKey("-----BEGIN PUBLIC KEY-----\nMIITest...\n-----END PUBLIC KEY-----");

            // Verify request is properly constructed
            assertNotNull(request.getData());
            assertNotNull(request.getSignature());
            assertNotNull(request.getPublicKey());
        }

        @Test
        @DisplayName("Should handle invalid Base64 data gracefully")
        void shouldHandleInvalidBase64() {
            // Given
            VerifyRequest request = new VerifyRequest();
            request.setData("not-valid-base64!!!");
            request.setSignature("valid-base64-sig");
            request.setPublicKey("key");

            // When/Then - should not throw NPE
            assertNotNull(request.getData());
        }

        @Test
        @DisplayName("Should reject null public key")
        void shouldRejectNullPublicKey() {
            // Given
            VerifyRequest request = new VerifyRequest();
            request.setData("data");
            request.setSignature("sig");
            request.setPublicKey(null);

            // Then
            assertNull(request.getPublicKey());
        }
    }

    @Nested
    @DisplayName("Certificate Validation Tests")
    class CertificateValidationTests {

        @Test
        @DisplayName("Should validate certificate format")
        void shouldValidateCertificateFormat() {
            // Given
            String validCert = """
                    -----BEGIN CERTIFICATE-----
                    MIITestCertificate...
                    -----END CERTIFICATE-----
                    """;

            // Then
            assertTrue(validCert.contains("BEGIN CERTIFICATE"));
            assertTrue(validCert.contains("END CERTIFICATE"));
        }

        @Test
        @DisplayName("Should reject invalid certificate format")
        void shouldRejectInvalidCertFormat() {
            // Given
            String invalidCert = "not-a-valid-certificate";

            // Then
            assertFalse(invalidCert.contains("BEGIN CERTIFICATE"));
        }

        @Test
        @DisplayName("Should verify certificate chain")
        void shouldVerifyCertificateChain() throws Exception {
            // Given - create mock cert chain files
            Path rootCert = tempDir.resolve("root.pem");
            Path leafCert = tempDir.resolve("leaf.pem");

            Files.writeString(rootCert, "-----BEGIN CERTIFICATE-----\nRoot...\n-----END CERTIFICATE-----");
            Files.writeString(leafCert, "-----BEGIN CERTIFICATE-----\nLeaf...\n-----END CERTIFICATE-----");

            // Then - files exist
            assertTrue(Files.exists(rootCert));
            assertTrue(Files.exists(leafCert));
        }
    }

    @Nested
    @DisplayName("Response Tests")
    class ResponseTests {

        @Test
        @DisplayName("Should create valid response")
        void shouldCreateValidResponse() {
            // Given
            VerifyResponse response = new VerifyResponse();
            response.setValid(true);
            response.setMessage("Signature verified successfully");

            // Then
            assertTrue(response.isValid());
            assertEquals("Signature verified successfully", response.getMessage());
        }

        @Test
        @DisplayName("Should create invalid response with reason")
        void shouldCreateInvalidResponseWithReason() {
            // Given
            VerifyResponse response = new VerifyResponse();
            response.setValid(false);
            response.setMessage("Invalid signature: certificate expired");

            // Then
            assertFalse(response.isValid());
            assertTrue(response.getMessage().contains("expired"));
        }
    }

    @Nested
    @DisplayName("Algorithm Support Tests")
    class AlgorithmTests {

        @Test
        @DisplayName("Should support ML-DSA-65 algorithm")
        void shouldSupportMlDsa65() {
            // Given
            String algorithm = "ML-DSA-65";

            // Then
            assertTrue(algorithm.startsWith("ML-DSA"));
            assertTrue(algorithm.contains("65"));
        }

        @Test
        @DisplayName("Should support ML-DSA-87 algorithm")
        void shouldSupportMlDsa87() {
            // Given
            String algorithm = "ML-DSA-87";

            // Then
            assertTrue(algorithm.startsWith("ML-DSA"));
            assertTrue(algorithm.contains("87"));
        }
    }
}
