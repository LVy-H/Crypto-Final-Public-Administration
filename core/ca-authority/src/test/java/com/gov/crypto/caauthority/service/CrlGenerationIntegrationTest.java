package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.IssuedCertificateRepository;
import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService.MlDsaLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CrlGenerationIntegrationTest {

    @Mock
    private CertificateAuthorityRepository caRepository;

    @Mock
    private IssuedCertificateRepository certRepository;

    @Mock
    private AuditLogService auditLogService;

    // Use REAL PqcCryptoService
    @Spy
    private PqcCryptoService pqcCryptoService = new PqcCryptoService();

    @InjectMocks
    private CaService caService;

    private Path tempDir;

    @BeforeEach
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory("ca_test_keys");
        ReflectionTestUtils.setField(caService, "caStoragePath", tempDir.toString());
        ReflectionTestUtils.setField(caService, "mtlsStoragePath", tempDir.toString());

        // No certificate verifier needed for CRL generation test
    }

    @Test
    public void testGenerateCrl_WithFreshKeys_ShouldSuccess() throws Exception {
        System.out.println("Generating fresh ML-DSA keys...");

        // 1. Generate Fresh Key Pair using PqcCryptoService
        MlDsaLevel level = MlDsaLevel.ML_DSA_65; // Use ML-DSA-65 (level 3)
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(level);

        // 2. Generate Self-Signed Cert
        X509Certificate cert = pqcCryptoService.generateSelfSignedCertificate(keyPair, "CN=Test CA", 30, level);

        // 3. Save Key to File (Simulation of disk storage)
        String keyPath = tempDir.resolve("test-ca.key").toString();
        String keyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());
        Files.writeString(Path.of(keyPath), keyPem);

        String certPem = pqcCryptoService.certificateToPem(cert);

        // 4. Mock CA Entity
        UUID caId = UUID.randomUUID();
        CertificateAuthority ca = new CertificateAuthority();
        ca.setId(caId);
        ca.setName("Test CA");
        ca.setAlgorithm("ML-DSA-65"); // Important: Should match level
        ca.setStatus(CaStatus.ACTIVE);
        ca.setPrivateKeyPath(keyPath);
        ca.setCertificate(certPem);

        when(caRepository.findById(caId)).thenReturn(Optional.of(ca));

        // 5. Mock Revoked Certificates
        IssuedCertificate revokedCert = new IssuedCertificate();
        revokedCert.setSerialNumber("ABC12345");
        revokedCert.setRevokedAt(LocalDateTime.now().minusDays(1));

        when(certRepository.findByIssuingCaAndStatus(ca, IssuedCertificate.CertStatus.REVOKED))
                .thenReturn(Collections.singletonList(revokedCert));

        // 6. Execute generateCrl
        System.out.println("Attemping to generate CRL...");
        String crlPem = caService.generateCrl(caId);

        System.out.println("CRL Generated successfully:\n" + crlPem);

        // Basic assertions
        assert crlPem != null;
        assert crlPem.contains("BEGIN X509 CRL");
    }
}
