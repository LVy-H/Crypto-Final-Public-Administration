package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.IssuedCertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HierarchicalCaService.
 * Tests CA hierarchy operations, certificate chain validation, and revocation.
 */
@ExtendWith(MockitoExtension.class)
class HierarchicalCaServiceTest {

    @Mock
    private CertificateAuthorityRepository caRepository;

    @Mock
    private IssuedCertificateRepository certRepository;

    @InjectMocks
    private HierarchicalCaService caService;

    private CertificateAuthority rootCa;
    private CertificateAuthority provincialCa;
    private CertificateAuthority districtRa;

    @BeforeEach
    void setUp() {
        // Inject configuration values
        ReflectionTestUtils.setField(caService, "caStoragePath", "build/tmp/unit-test/ca");
        ReflectionTestUtils.setField(caService, "mtlsStoragePath", "build/tmp/unit-test/mtls");

        // Ensure directories exist
        new java.io.File("build/tmp/unit-test/ca").mkdirs();
        new java.io.File("build/tmp/unit-test/mtls").mkdirs();

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
        ca.setCertificate("-----BEGIN CERTIFICATE-----\nMIITestCert...\n-----END CERTIFICATE-----");
        ca.setPrivateKeyPath("/secure/ca/" + name.toLowerCase().replace(" ", "-") + "-key.pem");
        ca.setSubjectDn("/CN=" + name + "/O=PQC System/C=VN");
        return ca;
    }

    @Nested
    @DisplayName("Root CA Initialization Tests")
    class RootCaTests {

        @Test
        @DisplayName("Should return existing root CA if already initialized")
        void shouldReturnExistingRootCa() throws Exception {
            // Given
            when(caRepository.findByHierarchyLevelAndStatus(0, CaStatus.ACTIVE))
                    .thenReturn(Optional.of(rootCa));

            // When
            CertificateAuthority result = caService.initializeRootCa("New Root CA");

            // Then
            assertNotNull(result);
            assertEquals("National Root CA", result.getName());
            assertEquals("ML-DSA-87", result.getAlgorithm());
            verify(caRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Provincial CA Creation Tests")
    class ProvincialCaTests {

        @Test
        @DisplayName("Should throw when parent is not Root CA")
        void shouldThrowWhenParentNotRoot() {
            // Given
            when(caRepository.findById(provincialCa.getId())).thenReturn(Optional.of(provincialCa));

            // When/Then
            assertThrows(RuntimeException.class,
                    () -> caService.createProvincialCa(provincialCa.getId(), "Test Province"));
        }

        @Test
        @DisplayName("Should throw when parent CA not found")
        void shouldThrowWhenParentNotFound() {
            // Given
            UUID randomId = UUID.randomUUID();
            when(caRepository.findById(randomId)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(RuntimeException.class, () -> caService.createProvincialCa(randomId, "Test Province"));
        }
    }

    @Nested
    @DisplayName("District RA Creation Tests")
    class DistrictRaTests {

        @Test
        @DisplayName("Should throw when parent is not Provincial CA")
        void shouldThrowWhenParentNotProvincial() {
            // Given
            when(caRepository.findById(rootCa.getId())).thenReturn(Optional.of(rootCa));

            // When/Then
            assertThrows(RuntimeException.class, () -> caService.createDistrictRa(rootCa.getId(), "Test District"));
        }
    }

    @Nested
    @DisplayName("Certificate Chain Tests")
    class CertificateChainTests {

        @Test
        @DisplayName("Should return complete certificate chain from District to Root")
        void shouldReturnCompleteChain() {
            // Given
            when(caRepository.findById(districtRa.getId())).thenReturn(Optional.of(districtRa));

            // When
            List<String> chain = caService.getCertificateChain(districtRa.getId());

            // Then
            assertNotNull(chain);
            assertEquals(3, chain.size()); // District -> Provincial -> Root
        }

        @Test
        @DisplayName("Should return single cert for Root CA")
        void shouldReturnSingleCertForRoot() {
            // Given
            when(caRepository.findById(rootCa.getId())).thenReturn(Optional.of(rootCa));

            // When
            List<String> chain = caService.getCertificateChain(rootCa.getId());

            // Then
            assertEquals(1, chain.size());
        }

        @Test
        @DisplayName("Should return empty chain for non-existent CA")
        void shouldReturnEmptyForNonExistent() {
            // Given
            when(caRepository.findById(any())).thenReturn(Optional.empty());

            // When
            List<String> chain = caService.getCertificateChain(UUID.randomUUID());

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
            // Given - mock findById for ALL CAs that will be looked up (including recursive
            // calls)
            when(caRepository.findById(provincialCa.getId())).thenReturn(Optional.of(provincialCa));
            when(caRepository.findById(districtRa.getId())).thenReturn(Optional.of(districtRa));
            when(caRepository.findByParentCa(provincialCa)).thenReturn(List.of(districtRa));
            when(caRepository.findByParentCa(districtRa)).thenReturn(List.of());
            when(certRepository.findByIssuingCa(any())).thenReturn(List.of());

            // When
            caService.revokeCa(provincialCa.getId(), "Security breach");

            // Then
            assertEquals(CaStatus.REVOKED, provincialCa.getStatus());
            assertEquals(CaStatus.REVOKED, districtRa.getStatus());
            verify(caRepository, times(2)).save(any()); // Provincial + District
        }

        @Test
        @DisplayName("Should throw when CA not found for revocation")
        void shouldThrowWhenCaNotFoundForRevocation() {
            // Given
            UUID randomId = UUID.randomUUID();
            when(caRepository.findById(randomId)).thenReturn(Optional.empty());

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
            when(caRepository.findById(rootCa.getId())).thenReturn(Optional.of(rootCa));
            when(caRepository.findByParentCa(rootCa)).thenReturn(List.of(provincialCa));
            when(caRepository.findByParentCa(provincialCa)).thenReturn(List.of(districtRa));
            when(caRepository.findByParentCa(districtRa)).thenReturn(List.of());

            // When
            List<CertificateAuthority> subordinates = caService.getAllSubordinates(rootCa.getId());

            // Then
            assertEquals(2, subordinates.size());
            assertTrue(subordinates.contains(provincialCa));
            assertTrue(subordinates.contains(districtRa));
        }

        @Test
        @DisplayName("Should return CAs by level")
        void shouldReturnCasByLevel() {
            // Given
            when(caRepository.findByHierarchyLevel(0)).thenReturn(List.of(rootCa));

            // When
            List<CertificateAuthority> roots = caService.getCasByLevel(0);

            // Then
            assertEquals(1, roots.size());
            assertEquals("National Root CA", roots.get(0).getName());
        }
    }
}
