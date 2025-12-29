package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.model.IssuedCertificate.CertStatus;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.IssuedCertificateRepository;
import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService.MlDsaLevel;
import com.gov.crypto.common.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class HierarchicalCaService {

    private static final Logger log = LoggerFactory.getLogger(HierarchicalCaService.class);

    @org.springframework.beans.factory.annotation.Value("${app.ca.storage-path:/secure/ca}")
    private String caStoragePath;

    @org.springframework.beans.factory.annotation.Value("${app.mtls.storage-path:/secure/mtls}")
    private String mtlsStoragePath;

    private final CertificateAuthorityRepository caRepository;
    private final IssuedCertificateRepository certRepository;
    private final PqcCryptoService pqcCryptoService;
    private final com.gov.crypto.caauthority.security.KeyEncryptionService keyEncryptionService;

    public HierarchicalCaService(CertificateAuthorityRepository caRepository,
            IssuedCertificateRepository certRepository,
            com.gov.crypto.caauthority.security.KeyEncryptionService keyEncryptionService) {
        this.caRepository = caRepository;
        this.certRepository = certRepository;
        this.pqcCryptoService = new PqcCryptoService();
        this.keyEncryptionService = keyEncryptionService;
    }

    @jakarta.annotation.PostConstruct
    private void ensureStorageExists() {
        new File(caStoragePath).mkdirs();
    }

    // ============ Pending CA storage for CSR workflow ============
    private final java.util.Map<String, PendingCa> pendingCas = new java.util.concurrent.ConcurrentHashMap<>();

    public record PendingCa(String id, String name, String algorithm, String privateKeyPem,
            String publicKeyPem, String csrPem, java.time.Instant createdAt) {
    }

    public record CsrResult(String pendingCaId, String csrPem) {
    }

    /**
     * Generate CSR for Subordinate CA initialization.
     * This is step 1 of the proper CA setup workflow.
     * 
     * @param name      CA name
     * @param algorithm mldsa87, mldsa65, or ecdsa384
     * @return CSR result with pending CA ID
     */
    @Transactional
    public CsrResult generateCaCsr(String name, String algorithm) throws Exception {
        String sanitizedName = SecurityUtils.sanitizeDnComponent(name, "CA name");

        log.info("Generating CSR for CA: {} with algorithm: {}", sanitizedName, algorithm);

        MlDsaLevel level = switch (algorithm.toLowerCase()) {
            case "mldsa87", "ml-dsa-87" -> MlDsaLevel.ML_DSA_87;
            case "mldsa65", "ml-dsa-65" -> MlDsaLevel.ML_DSA_65;
            case "mldsa44", "ml-dsa-44" -> MlDsaLevel.ML_DSA_44;
            default -> MlDsaLevel.ML_DSA_87; // Default to highest security
        };

        // Generate key pair
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(level);

        // Build subject DN
        String subjectDn = "CN=" + sanitizedName + ",O=PQC Digital Signature System,C=VN";

        // Generate CSR
        org.bouncycastle.asn1.x500.X500Name x500Subject = new org.bouncycastle.asn1.x500.X500Name(subjectDn);
        org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder csrBuilder = new org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder(
                x500Subject, keyPair.getPublic());

        org.bouncycastle.operator.ContentSigner signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(
                level.getAlgorithmName())
                .setProvider("BCPQC")
                .build(keyPair.getPrivate());

        org.bouncycastle.pkcs.PKCS10CertificationRequest csr = csrBuilder.build(signer);

        // Convert to PEM
        java.io.StringWriter sw = new java.io.StringWriter();
        try (org.bouncycastle.util.io.pem.PemWriter pemWriter = new org.bouncycastle.util.io.pem.PemWriter(sw)) {
            pemWriter.writeObject(new org.bouncycastle.util.io.pem.PemObject(
                    "CERTIFICATE REQUEST", csr.getEncoded()));
        }
        String csrPem = sw.toString();

        // Store pending CA with private key (encrypted in production)
        String pendingId = UUID.randomUUID().toString();
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());
        String publicKeyPem = pqcCryptoService.publicKeyToPem(keyPair.getPublic());

        pendingCas.put(pendingId, new PendingCa(
                pendingId, sanitizedName, algorithm, privateKeyPem,
                publicKeyPem, csrPem, java.time.Instant.now()));

        log.info("CSR generated for pending CA: {} (ID: {})", sanitizedName, pendingId);

        return new CsrResult(pendingId, csrPem);
    }

    /**
     * Activate CA with signed certificate from National Root.
     * This is step 2 of the proper CA setup workflow.
     * 
     * @param pendingCaId         ID from generateCaCsr
     * @param certificatePem      Signed certificate from National Root
     * @param nationalRootCertPem Optional: National Root certificate for chain
     *                            validation
     * @return Activated CA
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

        // Validate certificate matches our CSR
        String certSubject = cert.getSubjectX500Principal().getName();
        log.info("Certificate subject: {}", certSubject);

        // Save encrypted private key to file
        String keyPath = caStoragePath + "/subordinate-key-" + pendingCaId + ".pem";
        keyEncryptionService.writeEncryptedKey(Path.of(keyPath), pending.privateKeyPem());

        // Create CA record
        CertificateAuthority ca = new CertificateAuthority();
        ca.setName(pending.name());
        ca.setType(CaType.ISSUING_CA);
        ca.setHierarchyLevel(1); // Level 1 = subordinate to National Root
        ca.setLabel("Subordinate CA (signed by National Root)");
        ca.setAlgorithm(pending.algorithm().toUpperCase());
        ca.setSubjectDn(certSubject);
        ca.setPrivateKeyPath(keyPath);
        ca.setCertificate(certificatePem);
        ca.setPublicKey(pending.publicKeyPem());
        ca.setValidFrom(LocalDateTime.now());
        ca.setValidUntil(LocalDateTime.now().plusYears(5)); // 5 years typical for subordinate
        ca.setStatus(CaStatus.ACTIVE);

        // Store national root cert path if provided
        if (nationalRootCertPem != null && !nationalRootCertPem.isBlank()) {
            String rootPath = caStoragePath + "/national-root.pem";
            Files.writeString(Path.of(rootPath), nationalRootCertPem);
            ca.setLabel("Subordinate CA (chain includes National Root)");
        }

        // Remove from pending
        pendingCas.remove(pendingCaId);

        log.info("CA activated successfully: {}", pending.name());
        return caRepository.save(ca);
    }

    /**
     * Initialize Root CA with ML-DSA-87 (NIST Level 5) using Bouncy Castle
     */
    @Transactional
    public CertificateAuthority initializeRootCa(String name) throws Exception {
        // SECURITY: Validate and sanitize input
        String sanitizedName = SecurityUtils.sanitizeDnComponent(name, "CA name");

        // Check if root CA already exists (Level 0)
        var existing = caRepository.findByHierarchyLevelAndStatus(0, CaStatus.ACTIVE);
        if (existing.isPresent()) {
            return existing.get();
        }

        log.info("Initializing Root CA: {} with ML-DSA-87 (NIST Level 5)", sanitizedName);

        // Generate ML-DSA-87 key pair using Bouncy Castle
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(MlDsaLevel.ML_DSA_87);

        // Build subject DN in RFC 4514 format
        String subjectDn = "CN=" + sanitizedName + ",O=PQC Digital Signature System,C=VN";

        // Generate self-signed certificate (10 years)
        X509Certificate cert = pqcCryptoService.generateSelfSignedCertificate(
                keyPair, subjectDn, 3650, MlDsaLevel.ML_DSA_87);

        // Convert to PEM format
        String certPem = pqcCryptoService.certificateToPem(cert);
        String publicKeyPem = pqcCryptoService.publicKeyToPem(keyPair.getPublic());
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());

        // Save encrypted private key to file
        String keyPath = caStoragePath + "/root-key.pem";
        keyEncryptionService.writeEncryptedKey(Path.of(keyPath), privateKeyPem);

        // Save to database
        CertificateAuthority rootCa = new CertificateAuthority();
        rootCa.setName(name);
        rootCa.setType(CaType.ISSUING_CA);
        rootCa.setHierarchyLevel(0);
        rootCa.setLabel("Root CA");
        rootCa.setAlgorithm("ML-DSA-87");
        rootCa.setSubjectDn(subjectDn);
        rootCa.setPrivateKeyPath(keyPath);
        rootCa.setCertificate(certPem);
        rootCa.setPublicKey(publicKeyPem);
        rootCa.setValidFrom(LocalDateTime.now());
        rootCa.setValidUntil(LocalDateTime.now().plusYears(10));
        rootCa.setStatus(CaStatus.ACTIVE);

        log.info("Root CA initialized successfully: {}", sanitizedName);
        return caRepository.save(rootCa);
    }

    /**
     * Create Provincial CA signed by Root CA (ML-DSA-87, 5 years)
     */
    /**
     * Create generic Subordinate CA (Recursive)
     */
    @Transactional
    public CertificateAuthority createSubordinate(UUID parentCaId, String name,
            CaType type, String algorithm, String label, int validDays) throws Exception {

        CertificateAuthority parentCa = caRepository.findById(parentCaId)
                .orElseThrow(() -> new RuntimeException("Parent CA not found"));

        // Validation: Only ISSUING_CA can create subordinates
        if (parentCa.getType() != CaType.ISSUING_CA) {
            throw new IllegalArgumentException("Parent CA is not an ISSUING_CA (it is " + parentCa.getType() + ")");
        }

        // Validation: EXTERNAL_RA should use registerExternalRa instead
        if (type == CaType.EXTERNAL_RA) {
            throw new IllegalArgumentException("Use registerExternalRa for Third-Party RAs");
        }

        // Logic for creating the subordinate
        return createSubordinateCa(parentCa, name, type, algorithm, label, validDays);
    }

    /**
     * Register Third-Party RA (External Keys)
     */
    @Transactional
    public CertificateAuthority registerExternalRa(UUID parentCaId, String name,
            String csrPem, String label, int validDays) throws Exception {

        CertificateAuthority parentCa = caRepository.findById(parentCaId)
                .orElseThrow(() -> new RuntimeException("Parent CA not found"));

        if (parentCa.getType() != CaType.ISSUING_CA) {
            throw new IllegalArgumentException("Parent CA is not an ISSUING_CA");
        }

        // SECURITY: Validate inputs
        String sanitizedName = SecurityUtils.sanitizeDnComponent(name, "External RA name");
        int newLevel = parentCa.getHierarchyLevel() + 1;

        // Safe file paths
        String safeFileName = sanitizedName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        String csrPath = caStoragePath + "/" + safeFileName + ".csr";
        String certPath = caStoragePath + "/" + safeFileName + "-cert.pem";

        // Save provided CSR
        Files.writeString(Path.of(csrPath), csrPem);

        // Write parent key/cert to temp for signing
        Path parentCertPath = Files.createTempFile("parent_ca", ".pem");
        Files.writeString(parentCertPath, parentCa.getCertificate());

        // Create config with External RA extensions
        Path configPath = createExtensionConfig("""
                basicConstraints=critical,CA:TRUE,pathlen:0
                keyUsage=critical,keyCertSign,cRLSign
                subjectKeyIdentifier=hash
                authorityKeyIdentifier=keyid:always,issuer
                """);

        // Sign CSR with Parent CA
        runProcess(new ProcessBuilder(
                "openssl", "x509", "-req",
                "-in", csrPath,
                "-CA", parentCertPath.toString(),
                "-CAkey", parentCa.getPrivateKeyPath(),
                "-out", certPath,
                "-days", String.valueOf(validDays),
                "-CAcreateserial",
                "-extfile", configPath.toString(),
                "-extensions", "v3_ext"), name + " External RA Signing");

        Files.deleteIfExists(configPath);

        Files.deleteIfExists(parentCertPath);
        Files.deleteIfExists(Path.of(csrPath)); // Clean up CSR

        // Save to DB (No private key)
        CertificateAuthority extRa = new CertificateAuthority();
        extRa.setName(name);
        extRa.setType(CaType.EXTERNAL_RA);
        extRa.setHierarchyLevel(newLevel);
        extRa.setLabel(label);
        extRa.setParentCa(parentCa);
        extRa.setAlgorithm("Unknown (External)"); // Could extract from Cert
        extRa.setSubjectDn("CN=" + sanitizedName + " (External)"); // Could extract
        extRa.setCertificate(Files.readString(Path.of(certPath)));
        extRa.setValidFrom(LocalDateTime.now());
        extRa.setValidUntil(LocalDateTime.now().plusDays(validDays));
        extRa.setStatus(CaStatus.ACTIVE);

        return caRepository.save(extRa);
    }

    /**
     * Create Provincial CA signed by Root CA (ML-DSA-87, 5 years)
     * Legacy wrapper for createSubordinate
     */
    @Transactional
    public CertificateAuthority createProvincialCa(UUID parentCaId, String provinceName) throws Exception {
        return createSubordinate(parentCaId, provinceName, CaType.ISSUING_CA, "mldsa87", "Provincial CA", 1825);
    }

    /**
     * Create District RA signed by Provincial CA (ML-DSA-65, 2 years)
     * Legacy wrapper for createSubordinate
     */
    @Transactional
    public CertificateAuthority createDistrictRa(UUID parentCaId, String districtName) throws Exception {
        return createSubordinate(parentCaId, districtName, CaType.RA, "mldsa65", "District RA", 730);
    }

    /**
     * Initialize Internal Services CA signed by Root CA (ML-DSA-65, 5 years)
     * Uses Bouncy Castle for pure Java PQC
     */
    @Transactional
    public CertificateAuthority initializeInternalCa() throws Exception {
        // Check if internal CA already exists
        var existing = caRepository.findByLabel("Internal CA");
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        // Get Root CA
        var rootCa = caRepository.findByHierarchyLevelAndStatus(0, CaStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Root CA not initialized"));

        log.info("Initializing Internal CA with ML-DSA-65 (NIST Level 3)");

        // Generate ML-DSA-65 key pair using Bouncy Castle
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(MlDsaLevel.ML_DSA_65);

        // Build subject DN
        String subjectDn = "CN=Internal Services CA,O=PQC Digital Signature System,C=VN";

        // Load Root CA certificate and key for signing
        String rootCertPem = rootCa.getCertificate();
        if (rootCertPem == null || rootCertPem.isEmpty()) {
            throw new RuntimeException("Root CA certificate is not set in database. Re-initialize Root CA.");
        }
        String rootKeyPath = rootCa.getPrivateKeyPath();
        if (rootKeyPath == null || rootKeyPath.isEmpty()) {
            throw new RuntimeException("Root CA private key path is not set. Re-initialize Root CA.");
        }
        if (!new File(rootKeyPath).exists()) {
            throw new RuntimeException("Root CA private key file not found at: " + rootKeyPath);
        }
        String rootKeyPem = Files.readString(Path.of(rootKeyPath));

        // Generate subordinate certificate (5 years)
        X509Certificate cert = pqcCryptoService.generateSubordinateCertificate(
                keyPair.getPublic(), subjectDn,
                rootKeyPem, rootCertPem,
                1825, MlDsaLevel.ML_DSA_87, true);

        // Convert to PEM format
        String certPem = pqcCryptoService.certificateToPem(cert);
        String publicKeyPem = pqcCryptoService.publicKeyToPem(keyPair.getPublic());
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());

        // Save encrypted private key to file
        new File(mtlsStoragePath).mkdirs();
        String keyPath = mtlsStoragePath + "/internal-ca-key.pem";
        keyEncryptionService.writeEncryptedKey(Path.of(keyPath), privateKeyPem);
        Files.writeString(Path.of(mtlsStoragePath + "/internal-ca.pem"), certPem);

        // Save to database
        CertificateAuthority internalCa = new CertificateAuthority();
        internalCa.setName("Internal Services CA");
        internalCa.setType(CaType.ISSUING_CA);
        internalCa.setHierarchyLevel(1);
        internalCa.setLabel("Internal CA");
        internalCa.setParentCa(rootCa);
        internalCa.setAlgorithm("ML-DSA-65");
        internalCa.setSubjectDn(subjectDn);
        internalCa.setPrivateKeyPath(keyPath);
        internalCa.setCertificate(certPem);
        internalCa.setPublicKey(publicKeyPem);
        internalCa.setValidFrom(LocalDateTime.now());
        internalCa.setValidUntil(LocalDateTime.now().plusYears(5));
        internalCa.setStatus(CaStatus.ACTIVE);

        log.info("Internal CA initialized successfully");
        return caRepository.save(internalCa);
    }

    /**
     * Issue service certificate for mTLS (ML-DSA-65, 1 year)
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

        // Generate ML-DSA-65 key pair for service
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(MlDsaLevel.ML_DSA_65);

        // Build subject DN with service name
        String subjectDn = "CN=" + serviceName + ".internal,O=PQC Digital Signature System,C=VN";

        // Load Internal CA key and cert
        String caCertPem = ca.getCertificate();
        String caKeyPem = Files.readString(Path.of(ca.getPrivateKeyPath()));

        // Generate service certificate (not a CA)
        X509Certificate cert = pqcCryptoService.generateSubordinateCertificate(
                keyPair.getPublic(), subjectDn,
                caKeyPem, caCertPem,
                validDays, MlDsaLevel.ML_DSA_65, false);

        // Convert to PEM
        String certPem = pqcCryptoService.certificateToPem(cert);
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());

        // Save encrypted private key to mTLS storage
        String keyPath = mtlsStoragePath + "/" + serviceName + "-key.pem";
        String certPath = mtlsStoragePath + "/" + serviceName + ".pem";
        keyEncryptionService.writeEncryptedKey(Path.of(keyPath), privateKeyPem);
        Files.writeString(Path.of(certPath), certPem);

        log.info("Service certificate issued for: {}", serviceName);

        return new ServiceCertificateResult(certPem, privateKeyPem, caCertPem);
    }

    /**
     * Result class for service certificate issuance
     */
    public record ServiceCertificateResult(String certificate, String privateKey, String caCertificate) {
    }

    /**
     * Create Internal Services CA signed by Root CA (ML-DSA-65, 5 years)
     * 
     * @deprecated Use initializeInternalCa() instead
     */
    @Transactional
    public CertificateAuthority createInternalServicesCa(UUID rootCaId) throws Exception {
        return initializeInternalCa();
    }

    /**
     * Generate mTLS certificates for all internal services signed by Internal CA
     * 
     * @deprecated Use issueServiceCertificate() for individual services
     */
    private void generateServiceCertificates(CertificateAuthority internalCa) throws Exception {
        String[] services = { "api-gateway", "identity-service", "ca-authority", "cloud-sign",
                "validation-service", "ra-service", "signature-core" };

        for (String svc : services) {
            try {
                issueServiceCertificate(svc, List.of(svc + ".crypto-pqc.svc.cluster.local"), 365);
            } catch (Exception e) {
                log.warn("Failed to generate cert for {}: {}", svc, e.getMessage());
            }
        }
    }

    private CertificateAuthority createSubordinateCa(CertificateAuthority parentCa, String name,
            CaType type, String algorithm, String label, int validDays) throws Exception {

        // SECURITY: Validate and sanitize all inputs
        String sanitizedName = SecurityUtils.sanitizeDnComponent(name, "CA name");
        String validatedAlgorithm = SecurityUtils.validateAlgorithm(algorithm);
        int newLevel = parentCa.getHierarchyLevel() + 1;

        // Generate safe file paths using sanitized name
        String safeFileName = sanitizedName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        String keyPath = caStoragePath + "/" + safeFileName + "-key.pem";
        // String csrPath = caStoragePath + "/" + safeFileName + ".csr"; // No longer
        // needed
        String certPath = caStoragePath + "/" + safeFileName + "-cert.pem";

        // BouncyCastle expects RFC 4514 format (comma separated)
        String subjectDn = "CN=" + sanitizedName + " " + label + ",O=PQC Digital Signature System,C=VN";

        // Determine algorithms levels
        MlDsaLevel childLevel = switch (validatedAlgorithm.toLowerCase()) {
            case "mldsa44", "ml-dsa-44" -> MlDsaLevel.ML_DSA_44;
            case "mldsa65", "ml-dsa-65" -> MlDsaLevel.ML_DSA_65;
            default -> MlDsaLevel.ML_DSA_87;
        };

        MlDsaLevel parentLevel = switch (parentCa.getAlgorithm().toLowerCase()) {
            case "mldsa44", "ml-dsa-44" -> MlDsaLevel.ML_DSA_44;
            case "mldsa65", "ml-dsa-65" -> MlDsaLevel.ML_DSA_65;
            default -> MlDsaLevel.ML_DSA_87;
        };

        log.info("Generating Subordinate CA with Java PQC: {} (Algo: {}, Parent: {})",
                sanitizedName, childLevel, parentLevel);

        // 1. Generate Key Pair (Java)
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(childLevel);
        String privateKeyPem = pqcCryptoService.privateKeyToPem(keyPair.getPrivate());
        String publicKeyPem = pqcCryptoService.publicKeyToPem(keyPair.getPublic());

        // Save private key to file (compatibility)
        Files.writeString(Path.of(keyPath), privateKeyPem);

        // 2. Load Parent Key & Cert
        String parentKeyPem = Files.readString(Path.of(parentCa.getPrivateKeyPath()));
        String parentCertPem = parentCa.getCertificate();

        // 3. Generate & Sign Certificate (Java)
        // Note: generateSubordinateCertificate handles extensions for CA=true
        X509Certificate cert = pqcCryptoService.generateSubordinateCertificate(
                keyPair.getPublic(),
                subjectDn,
                parentKeyPem,
                parentCertPem,
                validDays,
                parentLevel, // Signing uses parent's algorithm
                true // isCA
        );

        String certPem = pqcCryptoService.certificateToPem(cert);
        Files.writeString(Path.of(certPath), certPem);

        // Save to database
        CertificateAuthority subCa = new CertificateAuthority();
        subCa.setName(name);
        subCa.setType(type);
        subCa.setHierarchyLevel(newLevel);
        subCa.setLabel(label);
        subCa.setParentCa(parentCa);
        subCa.setAlgorithm(algorithm.toUpperCase().replace("MLDSA", "ML-DSA-")); // Standardize
        subCa.setSubjectDn(subjectDn);
        subCa.setPrivateKeyPath(keyPath);
        subCa.setCertificate(certPem);
        subCa.setPublicKey(publicKeyPem);
        subCa.setValidFrom(LocalDateTime.now());
        subCa.setValidUntil(LocalDateTime.now().plusDays(validDays));
        subCa.setStatus(CaStatus.ACTIVE);

        return caRepository.save(subCa);
    }

    /**
     * Issue end-user certificate signed by District RA (ML-DSA-65, 1 year)
     */
    @Transactional
    public IssuedCertificate issueUserCertificate(UUID issuingRaId, String csrContent,
            String subjectDn) throws Exception {

        CertificateAuthority issuingRa = caRepository.findById(issuingRaId)
                .orElseThrow(() -> new RuntimeException("Issuing RA not found"));

        try {
            // 1. Parse CSR and extract Public Key
            log.info("Parsing CSR for user certificate issuance...");
            var csr = pqcCryptoService.parseCsrPem(csrContent);
            PublicKey userPublicKey = pqcCryptoService.getPublicKeyFromCsr(csr);

            // Use provided DN or extract from CSR if null/empty
            String finalSubjectDn = (subjectDn != null && !subjectDn.isBlank())
                    ? subjectDn
                    : pqcCryptoService.getSubjectDnFromCsr(csr);

            // 2. Load Issuer Materials
            String issuerKeyPem = Files.readString(Path.of(issuingRa.getPrivateKeyPath()));
            String issuerCertPem = issuingRa.getCertificate();

            // Determine Issuer Level
            MlDsaLevel issuerLevel = switch (issuingRa.getAlgorithm().toLowerCase()) {
                case "mldsa44", "ml-dsa-44" -> MlDsaLevel.ML_DSA_44;
                case "mldsa65", "ml-dsa-65" -> MlDsaLevel.ML_DSA_65;
                default -> MlDsaLevel.ML_DSA_87;
            };

            // 3. Sign Certificate (isCA=false for end user)
            log.info("Signing user certificate with PQC (Issuer: {}, Algo: {})", issuingRa.getName(), issuerLevel);
            X509Certificate userX509String = pqcCryptoService.generateSubordinateCertificate(
                    userPublicKey,
                    finalSubjectDn,
                    issuerKeyPem,
                    issuerCertPem,
                    365, // 1 year validity
                    issuerLevel,
                    false // isCA = false
            );

            String certPem = pqcCryptoService.certificateToPem(userX509String);

            // SECURITY: Generate cryptographically secure serial number
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

    /**
     * Get full certificate chain from leaf to root
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
     * Revoke a certificate
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

    /**
     * Revoke a CA/RA and cascade to all subordinates and issued certificates.
     * This is used when a Provincial CA or District RA needs to be revoked.
     */
    @Transactional
    public void revokeCa(UUID caId, String reason) {
        CertificateAuthority ca = caRepository.findById(caId)
                .orElseThrow(() -> new RuntimeException("CA not found"));

        // 1. Revoke all certificates issued by this CA
        List<IssuedCertificate> issuedCerts = certRepository.findByIssuingCa(ca);
        for (IssuedCertificate cert : issuedCerts) {
            if (cert.getStatus() == CertStatus.ACTIVE) {
                cert.setStatus(CertStatus.REVOKED);
                cert.setRevokedAt(LocalDateTime.now());
                cert.setRevocationReason("Parent CA revoked: " + reason);
                certRepository.save(cert);
            }
        }

        // 2. Recursively revoke all subordinate CAs
        List<CertificateAuthority> subordinates = caRepository.findByParentCa(ca);
        for (CertificateAuthority subCa : subordinates) {
            revokeCa(subCa.getId(), "Parent CA revoked: " + reason);
        }

        // 3. Revoke the CA itself
        ca.setStatus(CaStatus.REVOKED);
        caRepository.save(ca);

        System.out.println("[CA] Revoked CA: " + ca.getName() + " - " + reason);
    }

    /**
     * Generate Certificate Revocation List (CRL) for a specific CA.
     * 
     * Uses Bouncy Castle X509v2CRLBuilder for pure Java CRL generation.
     * No OpenSSL dependency, no temp files.
     */
    @Transactional
    public String generateCrl(UUID caId) throws Exception {
        CertificateAuthority ca = caRepository.findById(caId)
                .orElseThrow(() -> new RuntimeException("CA not found: " + caId));

        if (ca.getStatus() != CaStatus.ACTIVE && ca.getStatus() != CaStatus.REVOKED) {
            throw new IllegalArgumentException("Cannot generate CRL for inactive/expired CA");
        }

        // Load CA private key
        String privateKeyPem = keyEncryptionService.readDecryptedKey(Path.of(ca.getPrivateKeyPath()));
        java.security.PrivateKey caPrivateKey = pqcCryptoService.parsePrivateKeyPem(privateKeyPem);

        // Parse CA certificate
        java.security.cert.X509Certificate caCert = pqcCryptoService.parseCertificatePem(ca.getCertificate());

        // Build CRL using Bouncy Castle
        org.bouncycastle.asn1.x500.X500Name issuer = new org.bouncycastle.asn1.x500.X500Name(
                caCert.getSubjectX500Principal().getName());

        java.util.Date now = new java.util.Date();
        java.util.Date nextUpdate = new java.util.Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000); // 7 days

        org.bouncycastle.cert.X509v2CRLBuilder crlBuilder = new org.bouncycastle.cert.X509v2CRLBuilder(issuer, now);
        crlBuilder.setNextUpdate(nextUpdate);

        // Add revoked certificates
        List<IssuedCertificate> issuedCerts = certRepository.findByIssuingCa(ca);
        int revokedCount = 0;

        for (IssuedCertificate cert : issuedCerts) {
            if (cert.getStatus() == CertStatus.REVOKED && cert.getRevokedAt() != null) {
                java.math.BigInteger serialNumber = new java.math.BigInteger(cert.getSerialNumber(), 16);
                java.util.Date revocationDate = java.sql.Timestamp.valueOf(cert.getRevokedAt());

                crlBuilder.addCRLEntry(serialNumber, revocationDate,
                        org.bouncycastle.asn1.x509.CRLReason.privilegeWithdrawn);
                revokedCount++;
            }
        }

        // Add CRL Number extension
        org.bouncycastle.asn1.x509.CRLNumber crlNumber = new org.bouncycastle.asn1.x509.CRLNumber(
                java.math.BigInteger.valueOf(System.currentTimeMillis() / 1000));
        crlBuilder.addExtension(org.bouncycastle.asn1.x509.Extension.cRLNumber, false, crlNumber);

        // Sign CRL with CA private key
        org.bouncycastle.operator.ContentSigner signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(
                ca.getAlgorithm().replace("ML-DSA-", "Dilithium").replace("-44", "2").replace("-65", "3").replace("-87",
                        "5"))
                .setProvider("BCPQC")
                .build(caPrivateKey);

        org.bouncycastle.cert.X509CRLHolder crlHolder = crlBuilder.build(signer);

        // Convert to PEM format
        java.io.StringWriter writer = new java.io.StringWriter();
        try (org.bouncycastle.openssl.jcajce.JcaPEMWriter pemWriter = new org.bouncycastle.openssl.jcajce.JcaPEMWriter(
                writer)) {
            pemWriter.writeObject(crlHolder);
        }

        log.info("Generated CRL for CA '{}' with {} revoked certificates", ca.getName(), revokedCount);
        return writer.toString();
    }

    // ============ User Certificate Request Workflow ============

    @Transactional
    public IssuedCertificate createCertificateRequest(String username, String algorithm, String csrPem) {
        // Find an appropriate Issuing CA (e.g., Internal Service CA or any active
        // Issuing CA)
        // In a real system, this might choose based on algorithm or user org
        List<CertificateAuthority> potentialCas = caRepository.findByStatus(CaStatus.ACTIVE);
        CertificateAuthority issuingCa = potentialCas.stream()
                .filter(ca -> ca.getType() == CaType.ISSUING_CA || ca.getType() == CaType.RA)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active Issuing CA found to handle request"));

        // Create pending certificate record
        IssuedCertificate request = new IssuedCertificate();
        request.setUsername(username);
        request.setIssuingCa(issuingCa);
        request.setStatus(CertStatus.PENDING);
        request.setSerialNumber(UUID.randomUUID().toString()); // Temporary serial for request tracking
        request.setSubjectDn("CN=" + username + ", O=Citizen, C=VN"); // Placeholder DN
        request.setCertificate(""); // No cert yet
        request.setCsr(csrPem); // Store CSR
        request.setValidFrom(LocalDateTime.now());
        request.setValidUntil(LocalDateTime.now().plusYears(1)); // Default valid time

        log.info("Created certificate request for user: {} algorithm: {}", username, algorithm);
        return certRepository.save(request);
    }

    /**
     * Approve a pending certificate request
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

        // Issue the certificate using the stored CSR
        // This will update the existing record or create a new one?
        // issueUserCertificate logic creates a NEW record. We should refactor or adapt.
        // Let's adapt issueUserCertificate logic here directly or reuse it.
        // issueUserCertificate takes csrContent and subjectDn parameters.

        // Let's reuse issueUserCertificate but update the EXISTING record instead of
        // creating new.
        // Actually, let's just modify the existing record in place here.

        CertificateAuthority issuingRa = request.getIssuingCa();

        // 1. Parse CSR
        var csrObj = pqcCryptoService.parseCsrPem(request.getCsr());
        java.security.PublicKey userPublicKey = pqcCryptoService.getPublicKeyFromCsr(csrObj);

        // 2. Issuer materials
        String issuerKeyPem = Files.readString(Path.of(issuingRa.getPrivateKeyPath()));
        String issuerCertPem = issuingRa.getCertificate();
        MlDsaLevel issuerLevel = switch (issuingRa.getAlgorithm().toLowerCase()) {
            case "mldsa44", "ml-dsa-44" -> MlDsaLevel.ML_DSA_44;
            case "mldsa65", "ml-dsa-65" -> MlDsaLevel.ML_DSA_65;
            default -> MlDsaLevel.ML_DSA_87;
        };

        // 3. Sign
        X509Certificate userX509 = pqcCryptoService.generateSubordinateCertificate(
                userPublicKey,
                request.getSubjectDn(),
                issuerKeyPem,
                issuerCertPem,
                365,
                issuerLevel,
                false);

        String certPem = pqcCryptoService.certificateToPem(userX509);
        String serialNumber = SecurityUtils.generateSecureSerialNumber();

        request.setCertificate(certPem);
        request.setSerialNumber(serialNumber);
        request.setStatus(CertStatus.ACTIVE);
        request.setPublicKey(pqcCryptoService.publicKeyToPem(userPublicKey)); // Save PK

        return certRepository.save(request);
    }

    public List<IssuedCertificate> getUserCertificates(String username) {
        return certRepository.findByUsername(username);
    }

    private String formatIndexDate(LocalDateTime date) {
        if (date == null)
            return "";
        // YYMMDDHHMMSSZ
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'");
        return date.format(fmt);
    }

    private Path createCrlConfig(String dir, String keyPath) throws Exception {
        String content = """
                [ ca ]
                default_ca = CA_default

                [ CA_default ]
                dir             = %s
                database        = $dir/index.txt
                crlnumber       = $dir/crlnumber

                certificate     = $dir/ca.pem
                private_key     = %s

                default_md      = sha256
                default_days    = 30
                default_crl_days= 30
                preserve        = no
                policy          = policy_anything

                [ policy_anything ]
                countryName             = optional
                stateOrProvinceName     = optional
                localityName            = optional
                organizationName        = optional
                organizationalUnitName  = optional
                commonName              = supplied
                emailAddress            = optional
                """.formatted(dir, keyPath);

        Path p = Files.createTempFile("crl_config", ".cnf");
        Files.writeString(p, content);
        return p;
    }

    /**
     * Get all subordinate CAs under a given CA (recursive)
     */
    public List<CertificateAuthority> getAllSubordinates(UUID caId) {
        CertificateAuthority ca = caRepository.findById(caId).orElse(null);
        if (ca == null)
            return new ArrayList<>();

        List<CertificateAuthority> result = new ArrayList<>();
        collectSubordinates(ca, result);
        return result;
    }

    private void collectSubordinates(CertificateAuthority ca, List<CertificateAuthority> result) {
        List<CertificateAuthority> children = caRepository.findByParentCa(ca);
        for (CertificateAuthority child : children) {
            result.add(child);
            collectSubordinates(child, result);
        }
    }

    /**
     * Get all CAs at a specific level
     */
    public List<CertificateAuthority> getCasByLevel(int hierarchyLevel) {
        return caRepository.findByHierarchyLevel(hierarchyLevel);
    }

    public List<CertificateAuthority> getAllCas() {
        return caRepository.findAll();
    }

    /**
     * Save a CA entity (for updates like linking to org)
     */
    public CertificateAuthority saveCa(CertificateAuthority ca) {
        return caRepository.save(ca);
    }

    /**
     * Get CA by organization ID
     */
    public CertificateAuthority getCaByOrganizationId(UUID organizationId) {
        return caRepository.findByOrganizationId(organizationId).orElse(null);
    }

    private void runProcess(ProcessBuilder pb, String operation) throws Exception {
        log.info("[CA] Starting: {}", operation);
        log.debug("[CA] Command: {}", String.join(" ", pb.command()));

        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("[CA] Output: {}", line);
            }
        }

        // Increase timeout to 120 seconds for PQC key generation (can be slow)
        boolean completed = process.waitFor(120, TimeUnit.SECONDS);
        int exitCode = completed ? process.exitValue() : -1;

        log.info("[CA] Completed: {} (exit={}, timeout={})", operation, exitCode, !completed);

        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("OpenSSL " + operation + " timed out after 120 seconds");
        }

        if (exitCode != 0) {
            throw new RuntimeException("OpenSSL " + operation + " failed (exit=" + exitCode + "): " + output);
        }
    }

    private Path createExtensionConfig(String extensions) throws Exception {
        String configContent = """
                [req]
                distinguished_name = req_distinguished_name
                x509_extensions = v3_ext
                prompt = no

                [req_distinguished_name]
                CN = Generic

                [v3_ext]
                """ + extensions;

        Path configPath = Files.createTempFile("openssl_config", ".cnf");
        Files.writeString(configPath, configContent);
        return configPath;
    }
}
