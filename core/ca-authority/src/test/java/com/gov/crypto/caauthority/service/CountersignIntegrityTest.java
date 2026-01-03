package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.Countersignature;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.CountersignatureRepository;
import com.gov.crypto.common.pqc.PqcCryptoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integrity and security tests for CountersignatureService.
 * Tests revocation enforcement and stamp lifecycle validation.
 */
@ExtendWith(MockitoExtension.class)
class CountersignIntegrityTest {

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

    private UUID stampId;
    private UUID officerId;
    private String originalDocumentHash;

    @BeforeEach
    void setUp() {
        stampId = UUID.randomUUID();
        officerId = UUID.randomUUID();
        originalDocumentHash = Base64.getEncoder().encodeToString(
                "original-document-content-hash".getBytes());
    }

    // === Stamp Retrieval Security Tests ===

    @Nested
    @DisplayName("Stamp Retrieval Security")
    class RetrievalTests {

        @Test
        @DisplayName("Should retrieve stamp by valid ID")
        void shouldRetrieveStampById() {
            // Given
            Countersignature stamp = createValidStamp();
            when(repository.findById(stampId)).thenReturn(Optional.of(stamp));

            // When
            Optional<Countersignature> result = countersignatureService.getStamp(stampId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(stampId, result.get().getId());
            assertEquals(Countersignature.Status.ACTIVE, result.get().getStatus());
        }

        @Test
        @DisplayName("Should return empty for non-existent stamp")
        void shouldReturnEmptyForNonExistentStamp() {
            // Given
            when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

            // When
            Optional<Countersignature> result = countersignatureService.getStamp(UUID.randomUUID());

            // Then
            assertTrue(result.isEmpty(), "Non-existent stamp should return empty");
        }

        @Test
        @DisplayName("Should include officer ID for audit trail")
        void shouldIncludeOfficerIdForAudit() {
            // Given
            Countersignature stamp = createValidStamp();
            stamp.setOfficerId(officerId);
            when(repository.findById(stampId)).thenReturn(Optional.of(stamp));

            // When
            Optional<Countersignature> result = countersignatureService.getStamp(stampId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(officerId, result.get().getOfficerId(),
                    "Stamp should include officer ID for audit trail");
        }
    }

    // === Revocation Enforcement Tests ===

    @Nested
    @DisplayName("Revocation Enforcement")
    class RevocationTests {

        @Test
        @DisplayName("Should revoke active stamp")
        void shouldRevokeActiveStamp() {
            // Given
            Countersignature stamp = createValidStamp();
            stamp.setStatus(Countersignature.Status.ACTIVE);
            when(repository.findById(stampId)).thenReturn(Optional.of(stamp));
            when(repository.save(any(Countersignature.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            countersignatureService.revokeStamp(stampId, officerId);

            // Then
            assertEquals(Countersignature.Status.REVOKED, stamp.getStatus(),
                    "Stamp should be marked as REVOKED");
            verify(repository).save(stamp);
        }

        @Test
        @DisplayName("Should throw when revoking non-existent stamp")
        void shouldThrowWhenRevokingNonExistentStamp() {
            // Given
            when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalArgumentException.class,
                    () -> countersignatureService.revokeStamp(UUID.randomUUID(), officerId),
                    "Should throw when revoking non-existent stamp");
        }

        @Test
        @DisplayName("Revoked stamp should not be trusted")
        void revokedStampShouldNotBeTrusted() {
            // Given
            Countersignature stamp = createValidStamp();
            stamp.setStatus(Countersignature.Status.REVOKED);
            when(repository.findById(stampId)).thenReturn(Optional.of(stamp));

            // When
            Optional<Countersignature> result = countersignatureService.getStamp(stampId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(Countersignature.Status.REVOKED, result.get().getStatus(),
                    "Retrieved stamp should show REVOKED status");
        }
    }

    // === Status Lifecycle Tests ===

    @Nested
    @DisplayName("Stamp Status Lifecycle")
    class StatusLifecycleTests {

        @Test
        @DisplayName("New stamp should be ACTIVE")
        void newStampShouldBeActive() {
            Countersignature stamp = new Countersignature();
            assertEquals(Countersignature.Status.ACTIVE, stamp.getStatus(),
                    "New stamp should default to ACTIVE status");
        }

        @Test
        @DisplayName("All valid statuses should exist")
        void allValidStatusesShouldExist() {
            Set<Countersignature.Status> statuses = EnumSet.allOf(Countersignature.Status.class);

            assertEquals(3, statuses.size());
            assertTrue(statuses.contains(Countersignature.Status.ACTIVE));
            assertTrue(statuses.contains(Countersignature.Status.REVOKED));
            assertTrue(statuses.contains(Countersignature.Status.EXPIRED));
        }

        @Test
        @DisplayName("Expired stamp should be marked accordingly")
        void expiredStampShouldBeMarked() {
            // Given
            Countersignature stamp = createValidStamp();
            stamp.setStatus(Countersignature.Status.EXPIRED);
            when(repository.findById(stampId)).thenReturn(Optional.of(stamp));

            // When
            Optional<Countersignature> result = countersignatureService.getStamp(stampId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(Countersignature.Status.EXPIRED, result.get().getStatus());
        }
    }

    // === Stamp Purpose Security Tests ===

    @Nested
    @DisplayName("Stamp Purpose Validation")
    class PurposeTests {

        @Test
        @DisplayName("All valid purposes should exist")
        void allValidPurposesShouldExist() {
            Set<Countersignature.StampPurpose> purposes = EnumSet.allOf(Countersignature.StampPurpose.class);

            assertEquals(4, purposes.size());
            assertTrue(purposes.contains(Countersignature.StampPurpose.OFFICIAL_VALIDATION));
            assertTrue(purposes.contains(Countersignature.StampPurpose.NOTARIZATION));
            assertTrue(purposes.contains(Countersignature.StampPurpose.APPROVAL));
            assertTrue(purposes.contains(Countersignature.StampPurpose.CERTIFICATION));
        }

        @Test
        @DisplayName("Default purpose should be OFFICIAL_VALIDATION")
        void defaultPurposeShouldBeOfficialValidation() {
            Countersignature stamp = new Countersignature();
            assertEquals(Countersignature.StampPurpose.OFFICIAL_VALIDATION, stamp.getPurpose(),
                    "Default purpose should be OFFICIAL_VALIDATION");
        }
    }

    // === Helper Methods ===

    private Countersignature createValidStamp() {
        Countersignature stamp = new Countersignature();
        stamp.setId(stampId);
        stamp.setDocumentHash(originalDocumentHash);
        stamp.setOfficerId(officerId);
        stamp.setStatus(Countersignature.Status.ACTIVE);
        stamp.setPurpose(Countersignature.StampPurpose.OFFICIAL_VALIDATION);
        stamp.setUserSignature(Base64.getEncoder().encodeToString("user-sig".getBytes()));
        stamp.setUserCertPem("-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----");
        stamp.setOfficerSignature(Base64.getEncoder().encodeToString("officer-sig".getBytes()));
        stamp.setOfficerCertPem("-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----");
        stamp.setStampedAt(Instant.now());
        return stamp;
    }
}
