package com.gov.crypto.caauthority.service;

import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService.MlDsaLevel;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
public class OfflineRootSignerTest {

    private PqcCryptoService pqcCryptoService = new PqcCryptoService();

    @Test
    public void signOffline() throws Exception {
        // Define Paths
        String rootDir = System.getProperty("user.dir");
        // Adjust rootDir if running from subproject
        if (rootDir.endsWith("ca-authority")) {
            rootDir = new File(rootDir).getParentFile().getParent();
        }

        Path offlineDir = Paths.get(rootDir, "tests", "offline_data");
        Files.createDirectories(offlineDir);

        Path csrPath = offlineDir.resolve("csr.pem");
        Path certPath = offlineDir.resolve("issuing_cert.pem");
        Path rootCertPath = offlineDir.resolve("root_cert.pem");
        Path rootKeyPath = offlineDir.resolve("root_key.pem");

        if (!Files.exists(csrPath)) {
            System.err.println("CSR file not found at: " + csrPath);
            // Don't fail if just checking compilation, but for flow verify it fails
            return;
        }

        System.out.println("=== OFFLINE ROOT CA SIMULATION ===");

        // 1. Load or Generate Root CA
        KeyPair rootKeyPair;
        X509Certificate rootCert;

        if (Files.exists(rootKeyPath) && Files.exists(rootCertPath)) {
            System.out.println("Loading existing Root CA...");
            rootCert = pqcCryptoService.parseCertificatePem(Files.readString(rootCertPath));
            String keyPem = Files.readString(rootKeyPath);
            rootKeyPair = new KeyPair(
                    rootCert.getPublicKey(),
                    pqcCryptoService.parsePrivateKeyPem(keyPem));
        } else {
            System.out.println("Generating NEW Root CA (ML-DSA-87)...");
            MlDsaLevel rootLevel = MlDsaLevel.ML_DSA_87;
            rootKeyPair = pqcCryptoService.generateMlDsaKeyPair(rootLevel);
            String subjectDn = "CN=National Quantum Root CA,O=Government of Vietnam,C=VN";
            rootCert = pqcCryptoService.generateSelfSignedCertificate(rootKeyPair, subjectDn, 7300, rootLevel);

            // Save Root
            Files.writeString(rootKeyPath, pqcCryptoService.privateKeyToPem(rootKeyPair.getPrivate()));
            Files.writeString(rootCertPath, pqcCryptoService.certificateToPem(rootCert));
        }

        // 2. Read CSR
        System.out.println("Reading CSR from: " + csrPath);
        String csrPem = Files.readString(csrPath);
        PKCS10CertificationRequest csr = pqcCryptoService.parseCsrPem(csrPem);
        PublicKey childPublicKey = pqcCryptoService.getPublicKeyFromCsr(csr);

        // 3. Sign CSR
        // Extract Subject DN from CSR? Or force a standard one?
        // standard one:
        String childSubjectDn = csr.getSubject().toString();
        System.out.println("Signing for Subject: " + childSubjectDn);

        MlDsaLevel rootLevel = MlDsaLevel.ML_DSA_87; // Assuming root is 87

        // Load Root Key (PEM string)
        String rootKeyPem = pqcCryptoService.privateKeyToPem(rootKeyPair.getPrivate());
        String rootCertPem = pqcCryptoService.certificateToPem(rootCert);

        X509Certificate childCert = pqcCryptoService.generateSubordinateCertificate(
                childPublicKey, childSubjectDn, rootKeyPem, rootCertPem,
                1825, rootLevel, true); // true = isCA

        // 4. Save Cert
        String childCertPem = pqcCryptoService.certificateToPem(childCert);
        Files.writeString(certPath, childCertPem);
        System.out.println("Signed Certificate saved to: " + certPath);
    }
}
