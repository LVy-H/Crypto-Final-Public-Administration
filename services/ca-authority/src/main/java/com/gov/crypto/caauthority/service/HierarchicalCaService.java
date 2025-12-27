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

    public HierarchicalCaService(CertificateAuthorityRepository caRepository,
            IssuedCertificateRepository certRepository) {
        this.caRepository = caRepository;
        this.certRepository = certRepository;
        this.pqcCryptoService = new PqcCryptoService();
    }

    @jakarta.annotation.PostConstruct
    private void ensureStorageExists() {
        new File(caStoragePath).mkdirs();
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

        // Save private key to file (for backward compatibility)
        String keyPath = caStoragePath + "/root-key.pem";
        Files.writeString(Path.of(keyPath), privateKeyPem);

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
     * Create Internal Services CA signed by Root CA (ML-DSA-65, 5 years)
     */
    @Transactional
    public CertificateAuthority createInternalServicesCa(UUID rootCaId) throws Exception {
        // Create Internal Services CA (Level 1, ISSUING_CA)
        CertificateAuthority internalCa = createSubordinate(rootCaId, "Internal Services CA",
                CaType.ISSUING_CA, "mldsa65", "Internal CA", 1825);

        // Generate mTLS certificates for all services
        generateServiceCertificates(internalCa);

        return internalCa;
    }

    /**
     * Generate mTLS certificates for all internal services signed by Internal CA
     */
    private void generateServiceCertificates(CertificateAuthority internalCa) throws Exception {
        String[] services = { "api-gateway", "identity-service", "ca-authority", "cloud-sign",
                "validation-service", "ra-service", "signature-core" };

        String mtlsPath = "/secure/mtls";
        new File(mtlsStoragePath).mkdirs();

        // Copy Internal CA cert to mTLS directory
        runProcess(new ProcessBuilder("cp", internalCa.getPrivateKeyPath(),
                mtlsStoragePath + "/internal-ca-key.pem"), "Copy Internal CA Key");

        Path internalCaCertPath = Files.createTempFile("internal_ca", ".pem");
        Files.writeString(internalCaCertPath, internalCa.getCertificate());
        runProcess(new ProcessBuilder("cp", internalCaCertPath.toString(),
                mtlsStoragePath + "/internal-ca.pem"), "Copy Internal CA Cert");

        for (String svc : services) {
            System.out.println("[mTLS] Generating certificate for: " + svc);

            String keyPath = mtlsStoragePath + "/" + svc + "-key.pem";
            String csrPath = mtlsStoragePath + "/" + svc + ".csr";
            String certPath = mtlsStoragePath + "/" + svc + ".pem";

            // Generate ML-DSA-65 key for service
            runProcess(new ProcessBuilder(
                    "openssl", "genpkey", "-algorithm", "mldsa65", "-out", keyPath),
                    svc + " Key Gen");

            // Generate CSR
            runProcess(new ProcessBuilder(
                    "openssl", "req", "-new", "-key", keyPath, "-out", csrPath,
                    "-subj", "/CN=" + svc + ".internal/O=PQC System/C=VN"),
                    svc + " CSR Gen");

            // Create config with Service extensions
            Path configPath = createExtensionConfig("""
                    basicConstraints=critical,CA:FALSE
                    keyUsage=critical,digitalSignature,keyEncipherment
                    extendedKeyUsage=serverAuth,clientAuth
                    subjectKeyIdentifier=hash
                    authorityKeyIdentifier=keyid,issuer
                    """);

            // Sign with Internal CA
            runProcess(new ProcessBuilder(
                    "openssl", "x509", "-req", "-in", csrPath,
                    "-CA", internalCaCertPath.toString(),
                    "-CAkey", internalCa.getPrivateKeyPath(),
                    "-out", certPath, "-days", "365", "-CAcreateserial",
                    "-extfile", configPath.toString(),
                    "-extensions", "v3_ext"),
                    svc + " Signing");

            Files.deleteIfExists(configPath);

            // Clean up CSR
            Files.deleteIfExists(Path.of(csrPath));
        }

        Files.deleteIfExists(internalCaCertPath);
        System.out.println("[mTLS] All service certificates generated in " + mtlsStoragePath);
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
        String csrPath = caStoragePath + "/" + safeFileName + ".csr";
        String certPath = caStoragePath + "/" + safeFileName + "-cert.pem";
        String subjectDn = "/CN=" + sanitizedName + " " + label + "/O=PQC Digital Signature System/C=VN";

        // Generate key pair with validated algorithm
        runProcess(new ProcessBuilder(
                "openssl", "genpkey",
                "-algorithm", validatedAlgorithm,
                "-out", keyPath), sanitizedName + " Key Generation");

        // Generate CSR
        runProcess(new ProcessBuilder(
                "openssl", "req", "-new",
                "-key", keyPath,
                "-out", csrPath,
                "-subj", subjectDn), name + " CSR Generation");

        // Write parent CA cert to temp file for signing
        Path parentCertPath = Files.createTempFile("parent_ca", ".pem");
        Files.writeString(parentCertPath, parentCa.getCertificate());

        // Create config with Sub CA extensions
        Path configPath = createExtensionConfig("""
                basicConstraints=critical,CA:TRUE,pathlen:0
                keyUsage=critical,keyCertSign,cRLSign
                subjectKeyIdentifier=hash
                authorityKeyIdentifier=keyid:always,issuer
                """);

        // Sign with parent CA
        runProcess(new ProcessBuilder(
                "openssl", "x509", "-req",
                "-in", csrPath,
                "-CA", parentCertPath.toString(),
                "-CAkey", parentCa.getPrivateKeyPath(),
                "-out", certPath,
                "-days", String.valueOf(validDays),
                "-CAcreateserial",
                "-extfile", configPath.toString(),
                "-extensions", "v3_ext"), name + " Signing");

        Files.deleteIfExists(configPath);

        Files.deleteIfExists(parentCertPath);

        // Extract public key
        Path pubKeyPath = Files.createTempFile("sub_pub", ".pem");
        runProcess(new ProcessBuilder(
                "openssl", "pkey", "-in", keyPath, "-pubout", "-out", pubKeyPath.toString()), "Extract Public Key");

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
        subCa.setCertificate(Files.readString(Path.of(certPath)));
        subCa.setPublicKey(Files.readString(pubKeyPath));
        subCa.setValidFrom(LocalDateTime.now());
        subCa.setValidUntil(LocalDateTime.now().plusDays(validDays));
        subCa.setStatus(CaStatus.ACTIVE);

        Files.deleteIfExists(pubKeyPath);
        Files.deleteIfExists(Path.of(csrPath));

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

        Path csrPath = Files.createTempFile("user", ".csr");
        Path certPath = Files.createTempFile("user", ".pem");

        Files.writeString(csrPath, csrContent);

        try {
            // Write issuing CA cert to temp file
            Path issuingCertPath = Files.createTempFile("issuing_ca", ".pem");
            Files.writeString(issuingCertPath, issuingRa.getCertificate());

            // Create config with End User extensions
            Path configPath = createExtensionConfig("""
                    basicConstraints=critical,CA:FALSE
                    keyUsage=critical,digitalSignature,nonRepudiation,keyEncipherment
                    extendedKeyUsage=clientAuth,emailProtection
                    subjectKeyIdentifier=hash
                    authorityKeyIdentifier=keyid,issuer
                    """);

            // Sign the CSR
            runProcess(new ProcessBuilder(
                    "openssl", "x509", "-req",
                    "-in", csrPath.toString(),
                    "-CA", issuingCertPath.toString(),
                    "-CAkey", issuingRa.getPrivateKeyPath(),
                    "-out", certPath.toString(),
                    "-days", "365",
                    "-CAcreateserial",
                    "-extfile", configPath.toString(),
                    "-extensions", "v3_ext"), "User Certificate Signing");

            Files.deleteIfExists(configPath);

            Files.deleteIfExists(issuingCertPath);

            String certificate = Files.readString(certPath);
            // SECURITY: Generate cryptographically secure serial number (RFC 5280
            // compliant)
            String serialNumber = SecurityUtils.generateSecureSerialNumber();

            IssuedCertificate userCert = new IssuedCertificate();
            userCert.setIssuingCa(issuingRa);
            userCert.setSubjectDn(subjectDn);
            userCert.setSerialNumber(serialNumber);
            userCert.setCertificate(certificate);
            userCert.setValidFrom(LocalDateTime.now());
            userCert.setValidUntil(LocalDateTime.now().plusYears(1));
            userCert.setStatus(CertStatus.ACTIVE);

            return certRepository.save(userCert);
        } finally {
            Files.deleteIfExists(csrPath);
            Files.deleteIfExists(certPath);
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
     * This creates an index.txt database from the SQL records and runs openssl ca
     * -gencrl.
     */
    @Transactional
    public String generateCrl(UUID caId) throws Exception {
        CertificateAuthority ca = caRepository.findById(caId)
                .orElseThrow(() -> new RuntimeException("CA not found"));

        if (ca.getStatus() != CaStatus.ACTIVE && ca.getStatus() != CaStatus.REVOKED) {
            throw new IllegalArgumentException("Cannot generate CRL for inactive/expired CA");
        }

        List<IssuedCertificate> issuedCerts = certRepository.findByIssuingCa(ca);

        // Prepare temporary directory for OpenSSL CA db
        Path caDbDir = Files.createTempDirectory("ca_db_" + ca.getName().replaceAll("\\s+", "_"));
        Path indexTxtPath = caDbDir.resolve("index.txt");
        Path crlnumberPath = caDbDir.resolve("crlnumber");
        Path caCertPath = caDbDir.resolve("ca.pem");
        Path caKeyPath = Path.of(ca.getPrivateKeyPath()); // Use existing key path

        try {
            // Write CA cert
            Files.writeString(caCertPath, ca.getCertificate());

            // Write crlnumber (initialize if needed, though openssl might handle it,
            // usually needed)
            Files.writeString(crlnumberPath, "1000"); // Start CRL number

            // Build index.txt content
            StringBuilder indexContent = new StringBuilder();
            for (IssuedCertificate cert : issuedCerts) {
                // Format: V/R ExpDate [RevDate] Serial unknown SubjectDN
                // Dates in YYMMDDHHMMSSZ format
                String status = (cert.getStatus() == CertStatus.REVOKED) ? "R" : "V";
                String expDate = formatIndexDate(cert.getValidUntil());
                String revDate = (cert.getStatus() == CertStatus.REVOKED)
                        ? formatIndexDate(cert.getRevokedAt())
                        : "";

                String line = String.format("%s\t%s\t%s\t%s\tunknown\t%s",
                        status,
                        expDate,
                        revDate,
                        cert.getSerialNumber(),
                        cert.getSubjectDn());
                indexContent.append(line).append("\n");
            }
            Files.writeString(indexTxtPath, indexContent.toString());

            // Create config for CRL generation
            Path configPath = createCrlConfig(caDbDir.toAbsolutePath().toString(), caKeyPath.toString());
            Path crlOutputPath = caDbDir.resolve("crl.pem");

            // Run OpenSSL to generate CRL
            // openssl ca -gencrl -config ...
            runProcess(new ProcessBuilder(
                    "openssl", "ca", "-gencrl",
                    "-config", configPath.toString(),
                    "-out", crlOutputPath.toString()), "CRL Generation");

            return Files.readString(crlOutputPath);

        } finally {
            // Cleanup temp dir (basic recursive delete ideally, or rely on OS temp cleanup)
            // For now, delete known files
            Files.deleteIfExists(indexTxtPath);
            Files.deleteIfExists(crlnumberPath);
            Files.deleteIfExists(caCertPath);
            // Config and CRL cleaned up if needed or return content first
        }
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
