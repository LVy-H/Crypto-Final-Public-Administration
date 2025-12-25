package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaLevel;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaStatus;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.model.IssuedCertificate.CertStatus;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.IssuedCertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class HierarchicalCaService {

    private static final String CA_STORAGE_PATH = "/secure/ca";

    private final CertificateAuthorityRepository caRepository;
    private final IssuedCertificateRepository certRepository;

    public HierarchicalCaService(CertificateAuthorityRepository caRepository,
            IssuedCertificateRepository certRepository) {
        this.caRepository = caRepository;
        this.certRepository = certRepository;
        ensureStorageExists();
    }

    private void ensureStorageExists() {
        new File(CA_STORAGE_PATH).mkdirs();
    }

    /**
     * Initialize Root CA with ML-DSA-87 (NIST Level 5)
     */
    @Transactional
    public CertificateAuthority initializeRootCa(String name) throws Exception {
        // Check if root CA already exists
        var existing = caRepository.findByLevelAndStatus(CaLevel.ROOT, CaStatus.ACTIVE);
        if (existing.isPresent()) {
            return existing.get();
        }

        String algorithm = "mldsa87"; // ML-DSA-87 for Root CA
        String keyPath = CA_STORAGE_PATH + "/root-key.pem";
        String certPath = CA_STORAGE_PATH + "/root-cert.pem";
        String subjectDn = "/CN=" + name + "/O=PQC Digital Signature System/C=VN";

        // Generate ML-DSA-87 key pair
        runProcess(new ProcessBuilder(
                "openssl", "genpkey",
                "-algorithm", algorithm,
                "-out", keyPath), "Root Key Generation");

        // Self-sign Root CA certificate (10 years)
        runProcess(new ProcessBuilder(
                "openssl", "req", "-new", "-x509",
                "-key", keyPath,
                "-out", certPath,
                "-days", "3650",
                "-subj", subjectDn), "Root Cert Generation");

        // Extract public key
        Path pubKeyPath = Files.createTempFile("root_pub", ".pem");
        runProcess(new ProcessBuilder(
                "openssl", "pkey", "-in", keyPath, "-pubout", "-out", pubKeyPath.toString()), "Extract Public Key");

        // Save to database
        CertificateAuthority rootCa = new CertificateAuthority();
        rootCa.setName(name);
        rootCa.setLevel(CaLevel.ROOT);
        rootCa.setAlgorithm("ML-DSA-87");
        rootCa.setSubjectDn(subjectDn);
        rootCa.setPrivateKeyPath(keyPath);
        rootCa.setCertificate(Files.readString(Path.of(certPath)));
        rootCa.setPublicKey(Files.readString(pubKeyPath));
        rootCa.setValidFrom(LocalDateTime.now());
        rootCa.setValidUntil(LocalDateTime.now().plusYears(10));
        rootCa.setStatus(CaStatus.ACTIVE);

        Files.deleteIfExists(pubKeyPath);
        return caRepository.save(rootCa);
    }

    /**
     * Create Provincial CA signed by Root CA (ML-DSA-87, 5 years)
     */
    @Transactional
    public CertificateAuthority createProvincialCa(UUID parentCaId, String provinceName) throws Exception {
        CertificateAuthority parentCa = caRepository.findById(parentCaId)
                .orElseThrow(() -> new RuntimeException("Parent CA not found"));

        if (parentCa.getLevel() != CaLevel.ROOT) {
            throw new RuntimeException("Provincial CA must be signed by Root CA");
        }

        return createSubordinateCa(parentCa, provinceName, CaLevel.PROVINCIAL, "mldsa87", 1825);
    }

    /**
     * Create District RA signed by Provincial CA (ML-DSA-65, 2 years)
     */
    @Transactional
    public CertificateAuthority createDistrictRa(UUID parentCaId, String districtName) throws Exception {
        CertificateAuthority parentCa = caRepository.findById(parentCaId)
                .orElseThrow(() -> new RuntimeException("Parent CA not found"));

        if (parentCa.getLevel() != CaLevel.PROVINCIAL) {
            throw new RuntimeException("District RA must be signed by Provincial CA");
        }

        return createSubordinateCa(parentCa, districtName, CaLevel.DISTRICT, "mldsa65", 730);
    }

    /**
     * Create Internal Services CA signed by Root CA (ML-DSA-65, 5 years)
     * This CA issues mTLS certificates for internal microservices.
     */
    @Transactional
    public CertificateAuthority createInternalServicesCa(UUID rootCaId) throws Exception {
        CertificateAuthority rootCa = caRepository.findById(rootCaId)
                .orElseThrow(() -> new RuntimeException("Root CA not found"));

        if (rootCa.getLevel() != CaLevel.ROOT) {
            throw new RuntimeException("Internal Services CA must be signed by Root CA");
        }

        // Check if Internal CA already exists
        var existing = caRepository.findByLevelAndStatus(CaLevel.INTERNAL, CaStatus.ACTIVE);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create Internal Services CA signed by Root (ML-DSA-65, 5 years)
        CertificateAuthority internalCa = createSubordinateCa(rootCa, "Internal Services CA",
                CaLevel.INTERNAL, "mldsa65", 1825);

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
        new File(mtlsPath).mkdirs();

        // Copy Internal CA cert to mTLS directory
        runProcess(new ProcessBuilder("cp", internalCa.getPrivateKeyPath(),
                mtlsPath + "/internal-ca-key.pem"), "Copy Internal CA Key");

        Path internalCaCertPath = Files.createTempFile("internal_ca", ".pem");
        Files.writeString(internalCaCertPath, internalCa.getCertificate());
        runProcess(new ProcessBuilder("cp", internalCaCertPath.toString(),
                mtlsPath + "/internal-ca.pem"), "Copy Internal CA Cert");

        for (String svc : services) {
            System.out.println("[mTLS] Generating certificate for: " + svc);

            String keyPath = mtlsPath + "/" + svc + "-key.pem";
            String csrPath = mtlsPath + "/" + svc + ".csr";
            String certPath = mtlsPath + "/" + svc + ".pem";

            // Generate ML-DSA-65 key for service
            runProcess(new ProcessBuilder(
                    "openssl", "genpkey", "-algorithm", "mldsa65", "-out", keyPath),
                    svc + " Key Gen");

            // Generate CSR
            runProcess(new ProcessBuilder(
                    "openssl", "req", "-new", "-key", keyPath, "-out", csrPath,
                    "-subj", "/CN=" + svc + ".internal/O=PQC System/C=VN"),
                    svc + " CSR Gen");

            // Sign with Internal CA
            runProcess(new ProcessBuilder(
                    "openssl", "x509", "-req", "-in", csrPath,
                    "-CA", internalCaCertPath.toString(),
                    "-CAkey", internalCa.getPrivateKeyPath(),
                    "-out", certPath, "-days", "365", "-CAcreateserial"),
                    svc + " Signing");

            // Clean up CSR
            Files.deleteIfExists(Path.of(csrPath));
        }

        Files.deleteIfExists(internalCaCertPath);
        System.out.println("[mTLS] All service certificates generated in " + mtlsPath);
    }

    private CertificateAuthority createSubordinateCa(CertificateAuthority parentCa, String name,
            CaLevel level, String algorithm, int validDays) throws Exception {

        String keyPath = CA_STORAGE_PATH + "/" + name.toLowerCase().replace(" ", "-") + "-key.pem";
        String csrPath = CA_STORAGE_PATH + "/" + name.toLowerCase().replace(" ", "-") + ".csr";
        String certPath = CA_STORAGE_PATH + "/" + name.toLowerCase().replace(" ", "-") + "-cert.pem";
        String subjectDn = "/CN=" + name + " " + level.name() + " CA/O=PQC Digital Signature System/C=VN";

        // Generate key pair
        runProcess(new ProcessBuilder(
                "openssl", "genpkey",
                "-algorithm", algorithm,
                "-out", keyPath), name + " Key Generation");

        // Generate CSR
        runProcess(new ProcessBuilder(
                "openssl", "req", "-new",
                "-key", keyPath,
                "-out", csrPath,
                "-subj", subjectDn), name + " CSR Generation");

        // Write parent CA cert to temp file for signing
        Path parentCertPath = Files.createTempFile("parent_ca", ".pem");
        Files.writeString(parentCertPath, parentCa.getCertificate());

        // Sign with parent CA
        runProcess(new ProcessBuilder(
                "openssl", "x509", "-req",
                "-in", csrPath,
                "-CA", parentCertPath.toString(),
                "-CAkey", parentCa.getPrivateKeyPath(),
                "-out", certPath,
                "-days", String.valueOf(validDays),
                "-CAcreateserial"), name + " Signing");

        Files.deleteIfExists(parentCertPath);

        // Extract public key
        Path pubKeyPath = Files.createTempFile("sub_pub", ".pem");
        runProcess(new ProcessBuilder(
                "openssl", "pkey", "-in", keyPath, "-pubout", "-out", pubKeyPath.toString()), "Extract Public Key");

        // Save to database
        CertificateAuthority subCa = new CertificateAuthority();
        subCa.setName(name);
        subCa.setLevel(level);
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

            // Sign the CSR
            runProcess(new ProcessBuilder(
                    "openssl", "x509", "-req",
                    "-in", csrPath.toString(),
                    "-CA", issuingCertPath.toString(),
                    "-CAkey", issuingRa.getPrivateKeyPath(),
                    "-out", certPath.toString(),
                    "-days", "365",
                    "-CAcreateserial"), "User Certificate Signing");

            Files.deleteIfExists(issuingCertPath);

            String certificate = Files.readString(certPath);
            String serialNumber = UUID.randomUUID().toString();

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
    public List<CertificateAuthority> getCasByLevel(CaLevel level) {
        return caRepository.findByLevel(level);
    }

    private void runProcess(ProcessBuilder pb, String operation) throws Exception {
        System.out.println("[CA] Starting: " + operation);
        System.out.println("[CA] Command: " + String.join(" ", pb.command()));

        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("[CA] Output: " + line);
            }
        }

        // Increase timeout to 120 seconds for PQC key generation (can be slow)
        boolean completed = process.waitFor(120, TimeUnit.SECONDS);
        int exitCode = completed ? process.exitValue() : -1;

        System.out.println("[CA] Completed: " + operation + " (exit=" + exitCode + ", timeout=" + !completed + ")");

        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("OpenSSL " + operation + " timed out after 120 seconds");
        }

        if (exitCode != 0) {
            throw new RuntimeException("OpenSSL " + operation + " failed (exit=" + exitCode + "): " + output);
        }
    }
}
