package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.Countersignature;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.caauthority.repository.CountersignatureRepository;
import com.gov.crypto.common.pqc.PqcCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for applying and verifying countersignatures (stamps).
 */
@Service
public class CountersignatureService {

    private static final Logger log = LoggerFactory.getLogger(CountersignatureService.class);

    private final CountersignatureRepository repository;
    private final CertificateAuthorityRepository caRepository;
    private final PqcCryptoService pqcService;
    private final TsaService tsaService;

    public CountersignatureService(CountersignatureRepository repository,
            CertificateAuthorityRepository caRepository,
            PqcCryptoService pqcService,
            TsaService tsaService) {
        this.repository = repository;
        this.caRepository = caRepository;
        this.pqcService = pqcService;
        this.tsaService = tsaService;
    }

    /**
     * Apply a countersignature (stamp) to a user-signed document.
     *
     * @param documentHash  Hash of the original document (Base64)
     * @param userSignature User's signature on the document (Base64)
     * @param userCertPem   User's certificate PEM
     * @param officerId     UUID of the officer applying the stamp
     * @param officerCaId   UUID of the CA/RA the officer is associated with
     * @param purpose       Purpose of the stamp
     * @return The created countersignature record
     */
    @Transactional
    public Countersignature applyStamp(String documentHash, String userSignature, String userCertPem,
            UUID officerId, UUID officerCaId,
            Countersignature.StampPurpose purpose) throws Exception {

        // Check if already stamped
        if (repository.existsByDocumentHashAndUserSignatureAndStatus(
                documentHash, userSignature, Countersignature.Status.ACTIVE)) {
            throw new IllegalStateException("This document has already been stamped");
        }

        // Get the officer's CA to sign with
        CertificateAuthority officerCa = caRepository.findById(officerCaId)
                .orElseThrow(() -> new IllegalArgumentException("Officer CA not found: " + officerCaId));

        // Verify user's signature first
        X509Certificate userCert = pqcService.parseCertificatePem(userCertPem);
        byte[] docHashBytes = Base64.getDecoder().decode(documentHash);
        byte[] userSigBytes = Base64.getDecoder().decode(userSignature);

        PqcCryptoService.MlDsaLevel userLevel = getMlDsaLevelFromCert(userCert);
        boolean userSigValid = pqcService.verify(docHashBytes, userSigBytes, userCert.getPublicKey(), userLevel);

        if (!userSigValid) {
            throw new IllegalArgumentException("User signature verification failed");
        }

        // Create the data to be signed by officer: hash(documentHash + userSignature)
        byte[] stampData = createStampData(documentHash, userSignature);

        // Get officer's private key and certificate from CA
        // Note: privateKeyPath field stores the file path to the private key PEM
        String keyPem = Files.readString(Path.of(officerCa.getPrivateKeyPath()));
        PrivateKey officerPrivateKey = pqcService.parsePrivateKeyPem(keyPem);
        X509Certificate officerCert = pqcService.parseCertificatePem(officerCa.getCertificate());
        PqcCryptoService.MlDsaLevel officerLevel = getMlDsaLevelFromCert(officerCert);

        // Sign with officer's key
        byte[] officerSig = pqcService.sign(stampData, officerPrivateKey, officerLevel);
        String officerSignatureB64 = Base64.getEncoder().encodeToString(officerSig);

        // Generate RFC 3161 timestamp
        String timestampToken = null;
        try {
            byte[] tsToken = tsaService.generateTimestamp(stampData);
            timestampToken = Base64.getEncoder().encodeToString(tsToken);
        } catch (Exception e) {
            log.warn("Failed to generate timestamp: {}", e.getMessage());
            // Continue without timestamp - it's optional
        }

        // Create and save the countersignature
        Countersignature stamp = new Countersignature();
        stamp.setDocumentHash(documentHash);
        stamp.setUserSignature(userSignature);
        stamp.setUserCertPem(userCertPem);
        stamp.setOfficerSignature(officerSignatureB64);
        stamp.setOfficerCertPem(officerCa.getCertificate());
        stamp.setOfficerId(officerId);
        stamp.setTimestampToken(timestampToken);
        stamp.setStampedAt(Instant.now());
        stamp.setPurpose(purpose);
        stamp.setStatus(Countersignature.Status.ACTIVE);

        return repository.save(stamp);
    }

    /**
     * Verify a countersignature.
     */
    public StampVerificationResult verifyStamp(String documentHash, String userSignature,
            String officerSignature, String officerCertPem) {
        try {
            // Verify officer's signature
            byte[] stampData = createStampData(documentHash, userSignature);
            byte[] officerSigBytes = Base64.getDecoder().decode(officerSignature);

            X509Certificate officerCert = pqcService.parseCertificatePem(officerCertPem);
            PqcCryptoService.MlDsaLevel level = getMlDsaLevelFromCert(officerCert);

            boolean valid = pqcService.verify(stampData, officerSigBytes, officerCert.getPublicKey(), level);

            if (valid) {
                // Check certificate validity
                try {
                    officerCert.checkValidity();
                    return new StampVerificationResult(true, "Stamp verified successfully", null);
                } catch (Exception e) {
                    return new StampVerificationResult(false, "Officer certificate expired", e.getMessage());
                }
            } else {
                return new StampVerificationResult(false, "Invalid officer signature", null);
            }
        } catch (Exception e) {
            log.error("Stamp verification failed", e);
            return new StampVerificationResult(false, "Verification error", e.getMessage());
        }
    }

    /**
     * Get a stamp by ID.
     */
    public Optional<Countersignature> getStamp(UUID stampId) {
        return repository.findById(stampId);
    }

    /**
     * Revoke a stamp.
     */
    @Transactional
    public void revokeStamp(UUID stampId, UUID revokedBy) {
        Countersignature stamp = repository.findById(stampId)
                .orElseThrow(() -> new IllegalArgumentException("Stamp not found: " + stampId));

        stamp.setStatus(Countersignature.Status.REVOKED);
        repository.save(stamp);

        log.info("Stamp {} revoked by {}", stampId, revokedBy);
    }

    /**
     * Create the data to be signed for a stamp.
     */
    private byte[] createStampData(String documentHash, String userSignature) throws Exception {
        String combined = documentHash + ":" + userSignature;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(combined.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Determine ML-DSA level from certificate algorithm.
     */
    private PqcCryptoService.MlDsaLevel getMlDsaLevelFromCert(X509Certificate cert) {
        String algo = cert.getPublicKey().getAlgorithm();
        if (algo.equalsIgnoreCase("Dilithium2") || algo.contains("44")) {
            return PqcCryptoService.MlDsaLevel.ML_DSA_44;
        } else if (algo.equalsIgnoreCase("Dilithium3") || algo.contains("65")) {
            return PqcCryptoService.MlDsaLevel.ML_DSA_65;
        } else if (algo.equalsIgnoreCase("Dilithium5") || algo.contains("87")) {
            return PqcCryptoService.MlDsaLevel.ML_DSA_87;
        }
        // Default to ML_DSA_65
        return PqcCryptoService.MlDsaLevel.ML_DSA_65;
    }

    /**
     * Result of stamp verification.
     */
    public record StampVerificationResult(boolean valid, String message, String details) {
    }
}
