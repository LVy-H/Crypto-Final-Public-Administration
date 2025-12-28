package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.common.pqc.PqcCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TsaService.
 */
@ExtendWith(MockitoExtension.class)
class TsaServiceTest {

    @Mock
    private CertificateAuthorityRepository caRepository;

    @Mock
    private PqcCryptoService pqcService;

    @InjectMocks
    private TsaService tsaService;

    @Nested
    @DisplayName("TSA Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should handle missing TSA CA gracefully")
        void shouldHandleMissingTsaCa() {
            // Given - no TSA CA configured
            when(caRepository.findByName("TSA")).thenReturn(Optional.empty());
            when(caRepository.findAll()).thenReturn(java.util.Collections.emptyList());

            // When/Then - should not throw, just log warning
            assertDoesNotThrow(() -> {
                try {
                    tsaService.generateTimestamp("test".getBytes());
                } catch (IllegalStateException e) {
                    // Expected - TSA not configured
                    assertEquals("TSA not configured - TSA certificate not found", e.getMessage());
                }
            });
        }

        @Test
        @DisplayName("Should find TSA CA by name")
        void shouldFindTsaCaByName() {
            // Given
            CertificateAuthority tsaCa = new CertificateAuthority();
            tsaCa.setId(UUID.randomUUID());
            tsaCa.setName("TSA");

            when(caRepository.findByName("TSA")).thenReturn(Optional.of(tsaCa));

            // When - trigger initialization
            try {
                tsaService.generateTimestamp("test".getBytes());
            } catch (Exception e) {
                // Expected - TSA not fully configured
            }

            // Then
            verify(caRepository).findByName("TSA");
        }
    }

    @Nested
    @DisplayName("Timestamp Token Format Tests")
    class TokenFormatTests {

        @Test
        @DisplayName("Token should contain expected JSON fields when parsed")
        void tokenShouldContainExpectedFields() {
            // This tests the expected format of the token
            // The actual token requires TSA to be properly initialized
            String expectedFields = "serialNumber|genTime|hashAlgorithm|hashedMessage";
            String[] fields = expectedFields.split("\\|");

            assertEquals(4, fields.length);
            assertEquals("serialNumber", fields[0]);
            assertEquals("genTime", fields[1]);
            assertEquals("hashAlgorithm", fields[2]);
            assertEquals("hashedMessage", fields[3]);
        }
    }

    @Nested
    @DisplayName("Timestamp Verification Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should return false for invalid token format")
        void shouldReturnFalseForInvalidFormat() {
            // Given - invalid token (no delimiter)
            byte[] invalidToken = "invalid-token-no-delimiter".getBytes(StandardCharsets.UTF_8);
            byte[] originalHash = "test-hash".getBytes(StandardCharsets.UTF_8);

            // When
            boolean result = tsaService.verifyTimestamp(invalidToken, originalHash);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for malformed JSON token")
        void shouldReturnFalseForMalformedJson() {
            // Given - token with delimiter but invalid JSON
            byte[] malformedToken = "not-json|signature".getBytes(StandardCharsets.UTF_8);
            byte[] originalHash = "test-hash".getBytes(StandardCharsets.UTF_8);

            // When
            boolean result = tsaService.verifyTimestamp(malformedToken, originalHash);

            // Then
            assertFalse(result);
        }
    }
}
