package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HierarchicalCaService (Facade).
 * Tests verify that facade correctly delegates to CaManagementService and
 * CertificateIssuanceService.
 */
@ExtendWith(MockitoExtension.class)
class HierarchicalCaServiceTest {

    @Mock
    private CaManagementService caManagement;

    @Mock
    private CertificateIssuanceService certIssuance;

    private HierarchicalCaService caService;

    private CertificateAuthority rootCa;
    private CertificateAuthority provincialCa;
    private CertificateAuthority districtRa;

    @BeforeEach
    void setUp() {
        // Create facade with mocked services
        caService = new HierarchicalCaService(caManagement, certIssuance);

        // Setup test CA hierarchy
        rootCa = createTestCa("National Root CA", CaType.ISSUING_CA, 0, "ML-DSA-87", null);
        provincialCa = createTestCa("Ho Chi Minh City", CaType.ISSUING_CA, 1, "ML-DSA-87", rootCa);
        districtRa = createTestCa("Quan 1", CaType.RA, 2, "ML-DSA-65", provincialCa);
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
        ca.setCertificate("MOCK_CERT_PEM");
        ca.setPublicKey("MOCK_PUBLIC_KEY_PEM");
        ca.setPrivateKeyPath("/mock/path/key.pem");
        ca.setSubjectDn("CN=" + name);
        return ca;
    }

    @Nested
    @DisplayName("Provincial CA Creation Tests")
    class ProvincialCaTests {

        @Test
        @DisplayName("Should throw when parent is RA (not ISSUING_CA)")
        void shouldThrowWhenParentNotIssuingCa() throws Exception {
            // Given - CaManagementService throws when parent is RA
            when(caManagement.createProvincialCa(districtRa.getId(), "Test Province"))
                    .thenThrow(new IllegalArgumentException("Parent CA is not an ISSUING_CA"));

            // When/Then
            assertThrows(IllegalArgumentException.class,
                    () -> caService.createProvincialCa(districtRa.getId(), "Test Province"));

            verify(caManagement).createProvincialCa(districtRa.getId(), "Test Province");
        }

        @Test
        @DisplayName("Should throw when parent CA not found")
        void shouldThrowWhenParentNotFound() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();
            when(caManagement.createProvincialCa(randomId, "Test Province"))
                    .thenThrow(new RuntimeException("Parent CA not found"));

            // When/Then
            assertThrows(RuntimeException.class, () -> caService.createProvincialCa(randomId, "Test Province"));
        }
    }

    @Nested
    @DisplayName("District RA Creation Tests")
    class DistrictRaTests {

        @Test
        @DisplayName("Should throw when parent is RA (not ISSUING_CA)")
        void shouldThrowWhenParentNotIssuingCa() throws Exception {
            // Given
            when(caManagement.createDistrictRa(districtRa.getId(), "Test District"))
                    .thenThrow(new IllegalArgumentException("Parent CA is not an ISSUING_CA"));

            // When/Then
            assertThrows(IllegalArgumentException.class,
                    () -> caService.createDistrictRa(districtRa.getId(), "Test District"));
        }
    }

    @Nested
    @DisplayName("Certificate Chain Tests")
    class CertificateChainTests {

        @Test
        @DisplayName("Should return complete certificate chain from District to Root")
        void shouldReturnCompleteChain() {
            // Given
            when(caManagement.getCertificateChain(districtRa.getId()))
                    .thenReturn(List.of("DISTRICT_CERT", "PROVINCIAL_CERT", "ROOT_CERT"));

            // When
            List<String> chain = caService.getCertificateChain(districtRa.getId());

            // Then
            assertNotNull(chain);
            assertEquals(3, chain.size());
            verify(caManagement).getCertificateChain(districtRa.getId());
        }

        @Test
        @DisplayName("Should return single cert for Root CA")
        void shouldReturnSingleCertForRoot() {
            // Given
            when(caManagement.getCertificateChain(rootCa.getId()))
                    .thenReturn(List.of("ROOT_CERT"));

            // When
            List<String> chain = caService.getCertificateChain(rootCa.getId());

            // Then
            assertEquals(1, chain.size());
        }

        @Test
        @DisplayName("Should return empty chain for non-existent CA")
        void shouldReturnEmptyForNonExistent() {
            // Given
            UUID randomId = UUID.randomUUID();
            when(caManagement.getCertificateChain(randomId)).thenReturn(List.of());

            // When
            List<String> chain = caService.getCertificateChain(randomId);

            // Then
            assertTrue(chain.isEmpty());
        }
    }

    @Nested
    @DisplayName("CA Revocation Tests")
    class RevocationTests {

        @Test
        @DisplayName("Should revoke CA and all subordinates")
        void shouldRevokeCaAndSubordinates() {
            // Given - just verify delegation happens
            doNothing().when(caManagement).revokeCa(provincialCa.getId(), "Security breach");

            // When
            caService.revokeCa(provincialCa.getId(), "Security breach");

            // Then
            verify(caManagement).revokeCa(provincialCa.getId(), "Security breach");
        }

        @Test
        @DisplayName("Should throw when CA not found for revocation")
        void shouldThrowWhenCaNotFoundForRevocation() {
            // Given
            UUID randomId = UUID.randomUUID();
            doThrow(new RuntimeException("CA not found")).when(caManagement).revokeCa(randomId, "Test reason");

            // When/Then
            assertThrows(RuntimeException.class, () -> caService.revokeCa(randomId, "Test reason"));
        }
    }

    @Nested
    @DisplayName("CA Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should return all subordinates recursively")
        void shouldReturnAllSubordinates() {
            // Given
            when(caManagement.getAllSubordinates(rootCa.getId()))
                    .thenReturn(List.of(provincialCa, districtRa));

            // When
            List<CertificateAuthority> subordinates = caService.getAllSubordinates(rootCa.getId());

            // Then
            assertEquals(2, subordinates.size());
            verify(caManagement).getAllSubordinates(rootCa.getId());
        }

        @Test
        @DisplayName("Should return CAs by level")
        void shouldReturnCasByLevel() {
            // Given
            when(caManagement.getCasByLevel(0)).thenReturn(List.of(rootCa));

            // When
            List<CertificateAuthority> roots = caService.getCasByLevel(0);

            // Then
            assertEquals(1, roots.size());
            assertEquals("National Root CA", roots.get(0).getName());
        }
    }
}
