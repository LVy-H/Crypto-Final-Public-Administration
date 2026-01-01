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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for CA hierarchy lifecycle management.
 * 
 * Responsibilities:
 * - CSR generation for offline signing workflow
 * - CA activation with signed certificates
 * - Subordinate CA creation
 * - External RA registration
 * - CA revocation (with cascade)
 * - Certificate chain queries
 * - CA hierarchy queries
 */
@Service
public class CaManagementService {

    private static final Logger log = LoggerFactory.getLogger(CaManagementService.class);

    private final CertificateAuthorityRepository caRepository;
    private final IssuedCertificateRepository certRepository;
    private final PqcCryptoService pqcCryptoService;
    private final KeyEncryptionService keyEncryptionService;
    private final String caStoragePath;

    // Pending CA storage for CSR workflow
    private final ConcurrentHashMap<String, PendingCa> pendingCas = new ConcurrentHashMap<>();

    public record PendingCa(String id, String name, String algorithm, String privateKeyPem,
            String publicKeyPem, String csrPem, Instant createdAt) {
    }

    public record CsrResult(String pendingCaId, String csrPem) {
    }

    /**
     * Constructor injection following Spring Boot best practices.
     * All dependencies are final and immutable after construction.
     */
    public CaManagementService(
            CertificateAuthorityRepository caRepository,
            IssuedCertificateRepository certRepository,
            KeyEncryptionService keyEncryptionService,
            @Value("${app.ca.storage-path:/secure/ca}") String caStoragePath) {
        this.caRepository = caRepository;
        this.certRepository = certRepository;
        this.keyEncryptionService = keyEncryptionService;
        this.caStoragePath = caStoragePath;
        this.pqcCryptoService = new PqcCryptoService();
    }

    @PostConstruct
    private void ensureStorageExists() {
        new File(caStoragePath).mkdirs();
    }

    // ========== CSR Workflow ==========

    /**
     * Generate CSR for Subordinate CA initialization (Step 1 of offline signing).
     */
    @Transactional
    public CsrResult generateCaCsr(String name, String algorithm) throws Exception {
        String sanitizedName = SecurityUtils.sanitizeDnComponent(name, "CA name");
        log.info("Generating CSR for CA: {} with algorithm: {}", sanitizedName, algorithm);

        MlDsaLevel level = parseAlgorithmLevel(algorithm);

        // Generate key pair
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(level);

        // Build subject DN
        String subjectDn = "CN=" + sanitizedName + ",O=PQC Digital Signature System,C=VN";

        // Generate CSR
        org.bouncycastle.asn1.x500.X500Name x500Subject = new org.bouncycastle.asn1.x500.X500Name(subjectDn);
        var csrBuilder = new org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder(
                x500Subject, keyPair.getPublic());

        var signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(level.getAlgorithmName())
                .setProvider("BC")
                .build(keyPair.getPrivate());

        var csr = csrBuilder.build(signer);

        // Convert to PEM
        java.io.StringWriter sw = new java.io.StringWriter();
        try (var pemWriter = new org.bouncycastle.util.io.pem.PemWriter(sw)) {
            pemWriter.writeObject(new org.bouncycastle.util.io.pem.PemObject(
                    "CERTIFICATE REQUEST", csr.getEncoded()));
        }
        String csrPem = sw.toString();

        // Store pending CA
        String pendingId = UUID.randomUUID().toString();
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());
        String publicKeyPem = pqcCryptoService.publicKeyToPem(keyPair.getPublic());

        pendingCas.put(pendingId, new PendingCa(
                pendingId, sanitizedName, algorithm, privateKeyPem,
                publicKeyPem, csrPem, Instant.now()));

        log.info("CSR generated for pending CA: {} (ID: {})", sanitizedName, pendingId);
        return new CsrResult(pendingId, csrPem);
    }

    /**
     * Activate CA with signed certificate (Step 2 of offline signing).
     */
    @Transactional
    public CertificateAuthority activateCaWithSignedCert(String pendingCaId,
            String certificatePem, String nationalRootCertPem) throws Exception {

        PendingCa pending = pendingCas.get(pendingCaId);
        if (pending == null) {
            throw new IllegalArgumentException("Pending CA not found: " + pendingCaId);
        }

        log.info("Activating CA: {} with signed certificate", pending.name());

        // Parse and validate the certificate
        X509Certificate cert = pqcCryptoService.parseCertificatePem(certificatePem);
        String certSubject = cert.getSubjectX500Principal().getName();
        log.info("Certificate subject: {}", certSubject);

        // Save encrypted private key
        String keyPath = caStoragePath + "/subordinate-key-" + pendingCaId + ".pem";
        keyEncryptionService.writeEncryptedKey(Path.of(keyPath), pending.privateKeyPem());

        // Create CA record
        CertificateAuthority ca = new CertificateAuthority();
        ca.setName(pending.name());
        ca.setType(CaType.ISSUING_CA);
        ca.setHierarchyLevel(1);
        ca.setLabel("Subordinate CA (signed by National Root)");
        ca.setAlgorithm(pending.algorithm().toUpperCase());
        ca.setSubjectDn(certSubject);
        ca.setPrivateKeyPath(keyPath);
        ca.setCertificate(certificatePem);
        ca.setPublicKey(pending.publicKeyPem());
        ca.setValidFrom(LocalDateTime.now());
        ca.setValidUntil(LocalDateTime.now().plusYears(5));
        ca.setStatus(CaStatus.ACTIVE);

        // Store national root cert if provided
        if (nationalRootCertPem != null && !nationalRootCertPem.isBlank()) {
            String rootPath = caStoragePath + "/national-root.pem";
            Files.writeString(Path.of(rootPath), nationalRootCertPem);
            ca.setLabel("Subordinate CA (chain includes National Root)");
        }

        pendingCas.remove(pendingCaId);
        log.info("CA activated successfully: {}", pending.name());
        return caRepository.save(ca);
    }

    // ========== CA Creation ==========

    /**
     * Create subordinate CA under a parent.
     */
    @Transactional
    public CertificateAuthority createSubordinate(UUID parentCaId, String name,
            CaType type, String algorithm, String label, int validDays) throws Exception {

        CertificateAuthority parentCa = caRepository.findById(parentCaId)
                .orElseThrow(() -> new RuntimeException("Parent CA not found"));

        if (parentCa.getType() != CaType.ISSUING_CA) {
            throw new IllegalArgumentException("Parent CA is not an ISSUING_CA (it is " + parentCa.getType() + ")");
        }

        if (type == CaType.EXTERNAL_RA) {
            throw new IllegalArgumentException("Use registerExternalRa for Third-Party RAs");
        }

        return createSubordinateCa(parentCa, name, type, algorithm, label, validDays);
    }

    /**
     * Register Third-Party RA with external keys.
     */
    @Transactional
    public CertificateAuthority registerExternalRa(UUID parentCaId, String name,
            String csrPem, String label, int validDays) throws Exception {

        CertificateAuthority parentCa = caRepository.findById(parentCaId)
                .orElseThrow(() -> new RuntimeException("Parent CA not found"));

        if (parentCa.getType() != CaType.ISSUING_CA) {
            throw new IllegalArgumentException("Parent CA is not an ISSUING_CA");
        }

        String sanitizedName = SecurityUtils.sanitizeDnComponent(name, "External RA name");
        int newLevel = parentCa.getHierarchyLevel() + 1;

        log.info("Registering External RA: {} (Parent: {})", sanitizedName, parentCa.getName());

        // Parse CSR and extract public key
        var csr = pqcCryptoService.parseCsrPem(csrPem);
        PublicKey externalPublicKey = pqcCryptoService.getPublicKeyFromCsr(csr);

        String algorithm = pqcCryptoService.getAlgorithmFromCsr(csr);
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = parentCa.getAlgorithm();
        }

        String subjectDn = pqcCryptoService.getSubjectDnFromCsr(csr);
        if (subjectDn == null || subjectDn.isEmpty()) {
            subjectDn = "CN=" + sanitizedName + " (External RA),O=PQC Digital Signature System,C=VN";
        }

        // Load parent credentials
        String parentKeyPem = keyEncryptionService.readDecryptedKey(Path.of(parentCa.getPrivateKeyPath()));
        String parentCertPem = parentCa.getCertificate();
        MlDsaLevel parentLevel = parseAlgorithmLevel(parentCa.getAlgorithm());

        // Sign certificate
        X509Certificate cert = pqcCryptoService.generateSubordinateCertificate(
                externalPublicKey, subjectDn, parentKeyPem, parentCertPem,
                validDays, parentLevel, true);

        String certPem = pqcCryptoService.certificateToPem(cert);

        // Save certificate to file
        String safeFileName = sanitizedName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        String certPath = caStoragePath + "/" + safeFileName + "-cert.pem";
        Files.writeString(Path.of(certPath), certPem);

        // Save to DB (no private key - it's external)
        CertificateAuthority extRa = new CertificateAuthority();
        extRa.setName(name);
        extRa.setType(CaType.EXTERNAL_RA);
        extRa.setHierarchyLevel(newLevel);
        extRa.setLabel(label);
        extRa.setParentCa(parentCa);
        extRa.setAlgorithm(algorithm);
        extRa.setSubjectDn(subjectDn);
        extRa.setCertificate(certPem);
        extRa.setValidFrom(LocalDateTime.now());
        extRa.setValidUntil(LocalDateTime.now().plusDays(validDays));
        extRa.setStatus(CaStatus.ACTIVE);

        log.info("External RA registered successfully: {}", sanitizedName);
        return caRepository.save(extRa);
    }

    /**
     * Create Provincial CA (legacy wrapper).
     */
    @Transactional
    public CertificateAuthority createProvincialCa(UUID parentCaId, String provinceName) throws Exception {
        return createSubordinate(parentCaId, provinceName, CaType.ISSUING_CA, "mldsa87", "Provincial CA", 1825);
    }

    /**
     * Create District RA (legacy wrapper).
     */
    @Transactional
    public CertificateAuthority createDistrictRa(UUID parentCaId, String districtName) throws Exception {
        return createSubordinate(parentCaId, districtName, CaType.RA, "mldsa65", "District RA", 730);
    }

    // ========== CA Revocation ==========

    /**
     * Revoke a CA and cascade to all subordinates and issued certificates.
     */
    @Transactional
    public void revokeCa(UUID caId, String reason) {
        CertificateAuthority ca = caRepository.findById(caId)
                .orElseThrow(() -> new RuntimeException("CA not found"));

        // Revoke all certificates issued by this CA
        List<IssuedCertificate> issuedCerts = certRepository.findByIssuingCa(ca);
        for (IssuedCertificate cert : issuedCerts) {
            if (cert.getStatus() == CertStatus.ACTIVE) {
                cert.setStatus(CertStatus.REVOKED);
                cert.setRevokedAt(LocalDateTime.now());
                cert.setRevocationReason("Parent CA revoked: " + reason);
                certRepository.save(cert);
            }
        }

        // Recursively revoke subordinate CAs
        List<CertificateAuthority> subordinates = caRepository.findByParentCa(ca);
        for (CertificateAuthority subCa : subordinates) {
            revokeCa(subCa.getId(), "Parent CA revoked: " + reason);
        }

        // Revoke the CA itself
        ca.setStatus(CaStatus.REVOKED);
        caRepository.save(ca);

        log.info("Revoked CA: {} - {}", ca.getName(), reason);
    }

    // ========== Query Methods ==========

    /**
     * Get full certificate chain from leaf to root.
     */
    public List<String> getCertificateChain(UUID caId) {
        List<String> chain = new ArrayList<>();
        CertificateAuthority current = caRepository.findById(caId).orElse(null);

        while (current != null) {
            chain.add(current.getCertificate());
            current = current.getParentCa();
        }

        return chain;
    }

    /**
     * Get all subordinate CAs (recursive).
     */
    public List<CertificateAuthority> getAllSubordinates(UUID caId) {
        CertificateAuthority ca = caRepository.findById(caId).orElse(null);
        if (ca == null)
            return new ArrayList<>();

        List<CertificateAuthority> result = new ArrayList<>();
        collectSubordinates(ca, result);
        return result;
    }

    public List<CertificateAuthority> getCasByLevel(int hierarchyLevel) {
        return caRepository.findByHierarchyLevel(hierarchyLevel);
    }

    public List<CertificateAuthority> getAllCas() {
        return caRepository.findAll();
    }

    public CertificateAuthority saveCa(CertificateAuthority ca) {
        return caRepository.save(ca);
    }

    public Optional<CertificateAuthority> getCaById(UUID caId) {
        return caRepository.findById(caId);
    }

    public CertificateAuthority getCaByOrganizationId(UUID organizationId) {
        return caRepository.findByOrganizationId(organizationId).orElse(null);
    }

    public Optional<CertificateAuthority> getCaByLabel(String label) {
        List<CertificateAuthority> results = caRepository.findByLabel(label);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ========== Private Helpers ==========

    private void collectSubordinates(CertificateAuthority ca, List<CertificateAuthority> result) {
        List<CertificateAuthority> children = caRepository.findByParentCa(ca);
        for (CertificateAuthority child : children) {
            result.add(child);
            collectSubordinates(child, result);
        }
    }

    private CertificateAuthority createSubordinateCa(CertificateAuthority parentCa, String name,
            CaType type, String algorithm, String label, int validDays) throws Exception {

        String sanitizedName = SecurityUtils.sanitizeDnComponent(name, "CA name");
        String validatedAlgorithm = SecurityUtils.validateAlgorithm(algorithm);
        int newLevel = parentCa.getHierarchyLevel() + 1;

        String safeFileName = sanitizedName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        String keyPath = caStoragePath + "/" + safeFileName + "-key.pem";
        String certPath = caStoragePath + "/" + safeFileName + "-cert.pem";
        String subjectDn = "CN=" + sanitizedName + " " + label + ",O=PQC Digital Signature System,C=VN";

        MlDsaLevel childLevel = parseAlgorithmLevel(validatedAlgorithm);
        MlDsaLevel parentLevel = parseAlgorithmLevel(parentCa.getAlgorithm());

        log.info("Generating Subordinate CA: {} (Algo: {}, Parent: {})", sanitizedName, childLevel, parentLevel);

        // Generate key pair
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(childLevel);
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());
        String publicKeyPem = pqcCryptoService.publicKeyToPem(keyPair.getPublic());
        Files.writeString(Path.of(keyPath), privateKeyPem);

        // Load parent credentials
        String parentKeyPem = Files.readString(Path.of(parentCa.getPrivateKeyPath()));
        String parentCertPem = parentCa.getCertificate();

        // Generate and sign certificate
        X509Certificate cert = pqcCryptoService.generateSubordinateCertificate(
                keyPair.getPublic(), subjectDn, parentKeyPem, parentCertPem,
                validDays, parentLevel, true);

        String certPem = pqcCryptoService.certificateToPem(cert);
        Files.writeString(Path.of(certPath), certPem);

        // Save to database
        CertificateAuthority subCa = new CertificateAuthority();
        subCa.setName(name);
        subCa.setType(type);
        subCa.setHierarchyLevel(newLevel);
        subCa.setLabel(label);
        subCa.setParentCa(parentCa);
        subCa.setAlgorithm(algorithm.toUpperCase().replace("MLDSA", "ML-DSA-"));
        subCa.setSubjectDn(subjectDn);
        subCa.setPrivateKeyPath(keyPath);
        subCa.setCertificate(certPem);
        subCa.setPublicKey(publicKeyPem);
        subCa.setValidFrom(LocalDateTime.now());
        subCa.setValidUntil(LocalDateTime.now().plusDays(validDays));
        subCa.setStatus(CaStatus.ACTIVE);

        return caRepository.save(subCa);
    }

    private MlDsaLevel parseAlgorithmLevel(String algorithm) {
        return switch (algorithm.toLowerCase()) {
            case "mldsa44", "ml-dsa-44" -> MlDsaLevel.ML_DSA_44;
            case "mldsa65", "ml-dsa-65" -> MlDsaLevel.ML_DSA_65;
            default -> MlDsaLevel.ML_DSA_87;
        };
    }
}
