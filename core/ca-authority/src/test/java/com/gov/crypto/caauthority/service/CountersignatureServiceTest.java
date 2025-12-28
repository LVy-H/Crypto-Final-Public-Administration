package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.Countersignature;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.CountersignatureRepository;
import com.gov.crypto.common.pqc.PqcCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CountersignatureService.
 */
@ExtendWith(MockitoExtension.class)
class CountersignatureServiceTest {

    @Mock
    private CountersignatureRepository repository;

    @Mock
    private CertificateAuthorityRepository caRepository;

    @Mock
    private PqcCryptoService pqcService;

    @Mock
    private TsaService tsaService;

    @InjectMocks
    private CountersignatureService countersignatureService;

    private UUID officerId;
    private UUID officerCaId;
    private UUID stampId;
    private String documentHash;
    private String userSignature;

    @BeforeEach
    void setUp() {
        officerId = UUID.randomUUID();
        officerCaId = UUID.randomUUID();
        stampId = UUID.randomUUID();

        // Base64-encoded test data
        documentHash = Base64.getEncoder().encodeToString("test-document-hash".getBytes());
        userSignature = Base64.getEncoder().encodeToString("test-user-signature".getBytes());
    }

    @Nested
    @DisplayName("Stamp Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should get stamp by ID")
        void shouldGetStampById() {
            // Given
            Countersignature stamp = new Countersignature();
            stamp.setId(stampId);
            stamp.setDocumentHash(documentHash);
            when(repository.findById(stampId)).thenReturn(Optional.of(stamp));

            // When
            Optional<Countersignature> result = countersignatureService.getStamp(stampId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(stampId, result.get().getId());
        }

        @Test
        @DisplayName("Should return empty when stamp not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(repository.findById(any())).thenReturn(Optional.empty());

            // When
            Optional<Countersignature> result = countersignatureService.getStamp(UUID.randomUUID());

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Stamp Revocation Tests")
    class RevocationTests {

        @Test
        @DisplayName("Should revoke stamp")
        void shouldRevokeStamp() {
            // Given
            Countersignature stamp = new Countersignature();
            stamp.setId(stampId);
            stamp.setStatus(Countersignature.Status.ACTIVE);
            when(repository.findById(stampId)).thenReturn(Optional.of(stamp));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            countersignatureService.revokeStamp(stampId, officerId);

            // Then
            assertEquals(Countersignature.Status.REVOKED, stamp.getStatus());
            verify(repository).save(stamp);
        }

        @Test
        @DisplayName("Should throw when revoking non-existent stamp")
        void shouldThrowWhenRevokingNonExistent() {
            // Given
            when(repository.findById(any())).thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalArgumentException.class,
                    () -> countersignatureService.revokeStamp(UUID.randomUUID(), officerId));
        }
    }

    @Nested
    @DisplayName("Stamp Purpose Tests")
    class PurposeTests {

        @Test
        @DisplayName("All stamp purposes should have valid names")
        void allPurposesShouldHaveValidNames() {
            for (Countersignature.StampPurpose purpose : Countersignature.StampPurpose.values()) {
                assertNotNull(purpose.name());
                assertFalse(purpose.name().isEmpty());
            }
        }

        @Test
        @DisplayName("Should have expected purposes")
        void shouldHaveExpectedPurposes() {
            assertEquals(4, Countersignature.StampPurpose.values().length);
            assertNotNull(Countersignature.StampPurpose.OFFICIAL_VALIDATION);
            assertNotNull(Countersignature.StampPurpose.NOTARIZATION);
            assertNotNull(Countersignature.StampPurpose.APPROVAL);
            assertNotNull(Countersignature.StampPurpose.CERTIFICATION);
        }
    }

    @Nested
    @DisplayName("Stamp Status Tests")
    class StatusTests {

        @Test
        @DisplayName("All stamp statuses should have valid names")
        void allStatusesShouldHaveValidNames() {
            for (Countersignature.Status status : Countersignature.Status.values()) {
                assertNotNull(status.name());
                assertFalse(status.name().isEmpty());
            }
        }

        @Test
        @DisplayName("Should have expected statuses")
        void shouldHaveExpectedStatuses() {
            assertEquals(3, Countersignature.Status.values().length);
            assertNotNull(Countersignature.Status.ACTIVE);
            assertNotNull(Countersignature.Status.REVOKED);
            assertNotNull(Countersignature.Status.EXPIRED);
        }
    }
}
