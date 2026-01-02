package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.IssuedCertificateRepository;
import com.gov.crypto.caauthority.security.KeyEncryptionService;
import com.gov.crypto.common.pqc.PqcCryptoService;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaServiceTest {

    @Mock
    private CertificateAuthorityRepository caRepository;

    @Mock
    private IssuedCertificateRepository certRepository;

    @Mock
    private KeyEncryptionService keyEncryptionService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PqcCryptoService pqcCryptoService;

    @Mock
    private CertificateVerifier certificateVerifier;

    @InjectMocks
    private CaService caService;

    private CertificateAuthority rootCa;

    @BeforeEach
    void setUp() {
        // Set storage path for test
        ReflectionTestUtils.setField(caService, "caStoragePath", "/tmp/ca-test");
        ReflectionTestUtils.setField(caService, "mtlsStoragePath", "/tmp/mtls-test");

        rootCa = new CertificateAuthority();
        rootCa.setId(UUID.randomUUID());
        rootCa.setName("National Root CA");
        rootCa.setType(CaType.ISSUING_CA);
        rootCa.setHierarchyLevel(0);
        rootCa.setAlgorithm("ML-DSA-87");
        rootCa.setStatus(CaStatus.ACTIVE);
        rootCa.setCertificate("DUMMY_ROOT_CERT_PEM");

        // Ensure directories exist
        new java.io.File("/tmp/ca-test").mkdirs();
        new java.io.File("/tmp/mtls-test").mkdirs();

        // Create dummy root key file
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/ca-test/root-key.pem"), "DUMMY_KEY_PEM");
            rootCa.setPrivateKeyPath("/tmp/ca-test/root-key.pem");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should create subordinate CA")
    void shouldCreateSubordinate() throws Exception {
        // Given
        UUID parentId = rootCa.getId();
        when(caRepository.findById(parentId)).thenReturn(Optional.of(rootCa));

        KeyPair mockKeyPair = mock(KeyPair.class);
        PublicKey mockPubKey = mock(PublicKey.class);
        when(mockKeyPair.getPublic()).thenReturn(mockPubKey);
        when(mockPubKey.getEncoded()).thenReturn(new byte[] { 1, 2, 3 });

        lenient().when(pqcCryptoService.generateMlDsaKeyPair(any())).thenReturn(mockKeyPair);
        lenient()
                .when(pqcCryptoService.generateSubordinateCertificate(any(), anyString(), anyString(), anyString(),
                        anyInt(),
                        any(), anyBoolean()))
                .thenReturn(mock(X509Certificate.class));

        lenient().when(pqcCryptoService.certificateToPem(any())).thenReturn("CERT_PEM");
        lenient().when(pqcCryptoService.privateKeyToPem(any())).thenReturn("KEY_PEM");
        lenient().when(pqcCryptoService.publicKeyToPem(any())).thenReturn("PUB_PEM");

        when(caRepository.save(any(CertificateAuthority.class))).thenAnswer(i -> {
            CertificateAuthority ca = i.getArgument(0);
            if (ca.getId() == null)
                ca.setId(UUID.randomUUID());
            return ca;
        });

        // When
        CertificateAuthority subCa = caService.createSubordinate(
                parentId, "Provincial CA", CaType.ISSUING_CA, "mldsa65", "Provincial CA", 365);

        // Then
        assertNotNull(subCa);
        assertEquals("Provincial CA", subCa.getName());
        assertEquals(1, subCa.getHierarchyLevel()); // Level 0 + 1
        assertEquals(rootCa, subCa.getParentCa());
        verify(auditLogService).logEvent(eq("CREATE_SUBORDINATE"), any(), any(), any());
    }

    @Test
    @DisplayName("Should returning CAs by level")
    void shouldGetCasByLevel() {
        // Given
        when(caRepository.findByHierarchyLevel(0)).thenReturn(List.of(rootCa));

        // When
        List<CertificateAuthority> result = caService.getCasByLevel(0);

        // Then
        assertEquals(1, result.size());
        assertEquals(rootCa, result.get(0));
    }

    @Test
    @DisplayName("Should revoke CA and cascade")
    void shouldRevokeCa() {
        // Given
        when(caRepository.findById(rootCa.getId())).thenReturn(Optional.of(rootCa));
        when(caRepository.findByParentCa(rootCa)).thenReturn(List.of()); // No subordinates for simplicity

        // When
        caService.revokeCa(rootCa.getId(), "Test Reason");

        // Then
        assertEquals(CaStatus.REVOKED, rootCa.getStatus());
        verify(caRepository).save(rootCa);
        verify(auditLogService).logEvent(eq("REVOKE_CA"), any(), any(), any());
    }

    @Test
    @DisplayName("Should build subject DN correctly")
    void shouldBuildSubjectDn() {
        String dn = com.gov.crypto.common.util.DnUtils.buildSubjectDn(
                "12345", "John Doe", "john@example.com", "City", "State", "Org", "VN");

        System.out.println("Built DN: " + dn);
        assertTrue(dn.contains("CN=John Doe"));
        assertTrue(dn.contains("SERIALNUMBER=12345"));
        assertTrue(dn.contains("C=VN"));
    }
}
