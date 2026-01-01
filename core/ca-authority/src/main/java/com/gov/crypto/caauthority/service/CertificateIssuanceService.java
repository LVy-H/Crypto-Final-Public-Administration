package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.model.IssuedCertificate.CertStatus;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.IssuedCertificateRepository;
import com.gov.crypto.caauthority.security.KeyEncryptionService;
import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService.MlDsaLevel;
import com.gov.crypto.common.security.SecurityUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for certificate lifecycle management.
 * 
 * Responsibilities:
 * - Issue user certificates
 * - Issue service certificates (mTLS)
 * - Certificate request workflow (create, approve)
 * - Certificate revocation
 * - CRL generation
 * - Certificate queries and statistics
 */
@Service
public class CertificateIssuanceService {

    private static final Logger log = LoggerFactory.getLogger(CertificateIssuanceService.class);

    private final CertificateAuthorityRepository caRepository;
    private final IssuedCertificateRepository certRepository;
    private final PqcCryptoService pqcCryptoService;
    private final KeyEncryptionService keyEncryptionService;
    private final String mtlsStoragePath;

    public record ServiceCertificateResult(String certificate, String privateKey, String caCertificate) {
    }

    /**
     * Constructor injection following Spring Boot best practices.
     */
    public CertificateIssuanceService(
            CertificateAuthorityRepository caRepository,
            IssuedCertificateRepository certRepository,
            KeyEncryptionService keyEncryptionService,
            @Value("${app.mtls.storage-path:/secure/mtls}") String mtlsStoragePath) {
        this.caRepository = caRepository;
        this.certRepository = certRepository;
        this.keyEncryptionService = keyEncryptionService;
        this.mtlsStoragePath = mtlsStoragePath;
        this.pqcCryptoService = new PqcCryptoService();
    }

    @PostConstruct
    private void ensureStorageExists() {
        new File(mtlsStoragePath).mkdirs();
    }

    // ========== Service Certificate Issuance ==========

    /**
     * Issue service certificate for mTLS.
     */
    @Transactional
    public ServiceCertificateResult issueServiceCertificate(String serviceName, List<String> dnsNames, int validDays)
            throws Exception {
        // Get Internal CA
        var internalCa = caRepository.findByLabel("Internal CA");
        if (internalCa.isEmpty()) {
            throw new RuntimeException("Internal CA not initialized. Call /api/v1/ca/internal/init first.");
        }
        CertificateAuthority ca = internalCa.get(0);

        log.info("Issuing service certificate for: {}", serviceName);

        // Generate ML-DSA-65 key pair
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(MlDsaLevel.ML_DSA_65);
        String subjectDn = "CN=" + serviceName + ".internal,O=PQC Digital Signature System,C=VN";

        // Load Internal CA credentials
        String caCertPem = ca.getCertificate();
        String caKeyPem = Files.readString(Path.of(ca.getPrivateKeyPath()));

        // Generate service certificate (not a CA)
        X509Certificate cert = pqcCryptoService.generateSubordinateCertificate(
                keyPair.getPublic(), subjectDn, caKeyPem, caCertPem,
                validDays, MlDsaLevel.ML_DSA_65, false);

        String certPem = pqcCryptoService.certificateToPem(cert);
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());

        // Save to mTLS storage
        String keyPath = mtlsStoragePath + "/" + serviceName + "-key.pem";
        String certPath = mtlsStoragePath + "/" + serviceName + ".pem";
        keyEncryptionService.writeEncryptedKey(Path.of(keyPath), privateKeyPem);
        Files.writeString(Path.of(certPath), certPem);

        log.info("Service certificate issued for: {}", serviceName);
        return new ServiceCertificateResult(certPem, privateKeyPem, caCertPem);
    }

    // ========== User Certificate Issuance ==========

    /**
     * Issue end-user certificate signed by District RA.
     */
    @Transactional
    public IssuedCertificate issueUserCertificate(UUID issuingRaId, String csrContent,
            String subjectDn) throws Exception {

        CertificateAuthority issuingRa = caRepository.findById(issuingRaId)
                .orElseThrow(() -> new RuntimeException("Issuing RA not found"));

        try {
            log.info("Parsing CSR for user certificate issuance...");
            var csr = pqcCryptoService.parseCsrPem(csrContent);
            PublicKey userPublicKey = pqcCryptoService.getPublicKeyFromCsr(csr);

            String finalSubjectDn = (subjectDn != null && !subjectDn.isBlank())
                    ? subjectDn
                    : pqcCryptoService.getSubjectDnFromCsr(csr);

            // Load issuer materials
            String issuerKeyPem = Files.readString(Path.of(issuingRa.getPrivateKeyPath()));
            String issuerCertPem = issuingRa.getCertificate();
            MlDsaLevel issuerLevel = parseAlgorithmLevel(issuingRa.getAlgorithm());

            // Sign certificate
            log.info("Signing user certificate (Issuer: {}, Algo: {})", issuingRa.getName(), issuerLevel);
            X509Certificate userX509 = pqcCryptoService.generateSubordinateCertificate(
                    userPublicKey, finalSubjectDn, issuerKeyPem, issuerCertPem,
                    365, issuerLevel, false);

            String certPem = pqcCryptoService.certificateToPem(userX509);
            String serialNumber = SecurityUtils.generateSecureSerialNumber();

            IssuedCertificate userCert = new IssuedCertificate();
            userCert.setIssuingCa(issuingRa);
            userCert.setSubjectDn(finalSubjectDn);
            userCert.setSerialNumber(serialNumber);
            userCert.setCertificate(certPem);
            userCert.setValidFrom(LocalDateTime.now());
            userCert.setValidUntil(LocalDateTime.now().plusYears(1));
            userCert.setStatus(CertStatus.ACTIVE);

            return certRepository.save(userCert);
        } catch (Exception e) {
            log.error("Failed to issue user certificate: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ========== Certificate Request Workflow ==========

    /**
     * Create a pending certificate request.
     */
    @Transactional
    public IssuedCertificate createCertificateRequest(String username, String algorithm, String csrPem) {
        List<CertificateAuthority> potentialCas = caRepository.findByStatus(CaStatus.ACTIVE);
        CertificateAuthority issuingCa = potentialCas.stream()
                .filter(ca -> ca.getType() == CaType.ISSUING_CA || ca.getType() == CaType.RA)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active Issuing CA found"));

        IssuedCertificate request = new IssuedCertificate();
        request.setUsername(username);
        request.setIssuingCa(issuingCa);
        request.setStatus(CertStatus.PENDING);
        request.setSerialNumber(UUID.randomUUID().toString());
        request.setSubjectDn("CN=" + username + ", O=Citizen, C=VN");
        request.setCertificate("");
        request.setCsr(csrPem);
        request.setValidFrom(LocalDateTime.now());
        request.setValidUntil(LocalDateTime.now().plusYears(1));

        log.info("Created certificate request for user: {} algorithm: {}", username, algorithm);
        return certRepository.save(request);
    }

    /**
     * Approve a pending certificate request.
     */
    @Transactional
    public IssuedCertificate approveCertificate(UUID requestId) throws Exception {
        IssuedCertificate request = certRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Certificate request not found"));

        if (request.getStatus() != CertStatus.PENDING) {
            throw new IllegalStateException("Certificate is not in PENDING status");
        }

        if (request.getCsr() == null || request.getCsr().isBlank()) {
            throw new IllegalStateException("No CSR found in request");
        }

        CertificateAuthority issuingRa = request.getIssuingCa();

        var csrObj = pqcCryptoService.parseCsrPem(request.getCsr());
        PublicKey userPublicKey = pqcCryptoService.getPublicKeyFromCsr(csrObj);

        String issuerKeyPem = Files.readString(Path.of(issuingRa.getPrivateKeyPath()));
        String issuerCertPem = issuingRa.getCertificate();
        MlDsaLevel issuerLevel = parseAlgorithmLevel(issuingRa.getAlgorithm());

        X509Certificate userX509 = pqcCryptoService.generateSubordinateCertificate(
                userPublicKey, request.getSubjectDn(), issuerKeyPem, issuerCertPem,
                365, issuerLevel, false);

        String certPem = pqcCryptoService.certificateToPem(userX509);
        String serialNumber = SecurityUtils.generateSecureSerialNumber();

        request.setCertificate(certPem);
        request.setSerialNumber(serialNumber);
        request.setStatus(CertStatus.ACTIVE);
        request.setPublicKey(pqcCryptoService.publicKeyToPem(userPublicKey));

        return certRepository.save(request);
    }

    // ========== Revocation ==========

    /**
     * Revoke a certificate.
     */
    @Transactional
    public void revokeCertificate(UUID certId, String reason) {
        IssuedCertificate cert = certRepository.findById(certId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        cert.setStatus(CertStatus.REVOKED);
        cert.setRevokedAt(LocalDateTime.now());
        cert.setRevocationReason(reason);
        certRepository.save(cert);
    }

    // ========== CRL Generation ==========

    /**
     * Generate CRL for a CA using Bouncy Castle.
     */
    @Transactional
    public String generateCrl(UUID caId) throws Exception {
        CertificateAuthority ca = caRepository.findById(caId)
                .orElseThrow(() -> new RuntimeException("CA not found: " + caId));

        if (ca.getStatus() != CaStatus.ACTIVE && ca.getStatus() != CaStatus.REVOKED) {
            throw new IllegalArgumentException("Cannot generate CRL for inactive/expired CA");
        }

        // Load CA private key
        String caKeyPem = Files.readString(Path.of(ca.getPrivateKeyPath()));
        PrivateKey issuerKey = pqcCryptoService.parsePrivateKeyPem(caKeyPem);

        // Parse issuer DN
        X509Certificate issuerCert = pqcCryptoService.parseCertificatePem(ca.getCertificate());
        X500Name issuerDn = new X500Name(issuerCert.getSubjectX500Principal().getName());

        // CRL validity
        Date now = new Date();
        Date nextUpdate = Date.from(LocalDateTime.now().plusDays(7)
                .atZone(ZoneId.systemDefault()).toInstant());

        // Build CRL
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(issuerDn, now);
        crlBuilder.setNextUpdate(nextUpdate);

        // Add all revoked certificates
        List<IssuedCertificate> revokedCerts = certRepository.findByIssuingCaAndStatus(ca, CertStatus.REVOKED);
        for (IssuedCertificate cert : revokedCerts) {
            Date revDate = cert.getRevokedAt() != null
                    ? Date.from(cert.getRevokedAt().atZone(ZoneId.systemDefault()).toInstant())
                    : now;
            crlBuilder.addCRLEntry(
                    new java.math.BigInteger(cert.getSerialNumber(), 16),
                    revDate,
                    CRLReason.unspecified);
        }

        // Sign CRL
        MlDsaLevel level = parseAlgorithmLevel(ca.getAlgorithm());
        var signer = new JcaContentSignerBuilder(level.getAlgorithmName())
                .setProvider("BC")
                .build(issuerKey);

        X509CRL crl = new JcaX509CRLConverter()
                .setProvider("BC")
                .getCRL(crlBuilder.build(signer));

        // Convert to PEM
        StringWriter sw = new StringWriter();
        try (var pemWriter = new org.bouncycastle.util.io.pem.PemWriter(sw)) {
            pemWriter.writeObject(new org.bouncycastle.util.io.pem.PemObject(
                    "X509 CRL", crl.getEncoded()));
        }

        log.info("Generated CRL for CA: {} with {} entries", ca.getName(), revokedCerts.size());
        return sw.toString();
    }

    // ========== Query Methods ==========

    public List<IssuedCertificate> getUserCertificates(String username) {
        return certRepository.findByUsername(username);
    }

    public List<IssuedCertificate> getAllIssuedCertificates() {
        return certRepository.findAll();
    }

    public List<IssuedCertificate> getCertificatesByStatus(CertStatus status) {
        return certRepository.findByStatus(status);
    }

    public Map<String, Long> getCertificateStats() {
        long total = certRepository.count();
        long active = certRepository.findByStatus(CertStatus.ACTIVE).size();
        long revoked = certRepository.findByStatus(CertStatus.REVOKED).size();
        long pending = certRepository.findByStatus(CertStatus.PENDING).size();
        return Map.of("total", total, "active", active, "revoked", revoked, "pending", pending);
    }

    // ========== Private Helpers ==========

    private MlDsaLevel parseAlgorithmLevel(String algorithm) {
        return switch (algorithm.toLowerCase()) {
            case "mldsa44", "ml-dsa-44" -> MlDsaLevel.ML_DSA_44;
            case "mldsa65", "ml-dsa-65" -> MlDsaLevel.ML_DSA_65;
            default -> MlDsaLevel.ML_DSA_87;
        };
    }
}
