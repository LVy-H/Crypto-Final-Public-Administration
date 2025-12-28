package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.repository.CertificateAuthorityRepository;
import com.gov.crypto.common.pqc.PqcCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;

/**
 * RFC 3161 Timestamp Authority Service.
 * 
 * The TSA certificate should be pre-signed by the offline Root CA
 * and imported into the system during initial setup.
 * 
 * Note: This is a simplified implementation. For production, use
 * BouncyCastle's full TSP (TimeStampProtocol) implementation.
 */
@Service
public class TsaService {

    private static final Logger log = LoggerFactory.getLogger(TsaService.class);

    private final CertificateAuthorityRepository caRepository;
    private final PqcCryptoService pqcService;
    private final SecureRandom secureRandom = new SecureRandom();

    // Cache for TSA certificate and key (loaded on first use)
    private X509Certificate tsaCertificate;
    private PrivateKey tsaPrivateKey;
    private boolean tsaInitialized = false;

    public TsaService(CertificateAuthorityRepository caRepository, PqcCryptoService pqcService) {
        this.caRepository = caRepository;
        this.pqcService = pqcService;
    }

    /**
     * Generate a signed timestamp token for the given data.
     * 
     * This is a simplified timestamp format:
     * {
     * "serialNumber": "...",
     * "genTime": "ISO-8601",
     * "hashAlgorithm": "SHA-256",
     * "hashedMessage": "base64",
     * "signature": "base64"
     * }
     *
     * @param messageImprint The hash of the data to be timestamped
     * @return The timestamp token bytes (JSON encoded, then signed)
     */
    public byte[] generateTimestamp(byte[] messageImprint) throws Exception {
        initializeTsa();

        if (tsaCertificate == null || tsaPrivateKey == null) {
            throw new IllegalStateException("TSA not configured - TSA certificate not found");
        }

        // Generate unique serial number
        BigInteger serialNumber = new BigInteger(64, secureRandom);
        Instant now = Instant.now();

        // Create timestamp info
        String timestampInfo = String.format(
                "{\"serialNumber\":\"%s\",\"genTime\":\"%s\",\"hashAlgorithm\":\"SHA-256\",\"hashedMessage\":\"%s\"}",
                serialNumber.toString(16),
                now.toString(),
                Base64.getEncoder().encodeToString(messageImprint));

        // Hash the timestamp info
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] infoHash = digest.digest(timestampInfo.getBytes(StandardCharsets.UTF_8));

        // Sign with TSA key
        PqcCryptoService.MlDsaLevel level = getMlDsaLevelFromCert(tsaCertificate);
        byte[] signature = pqcService.sign(infoHash, tsaPrivateKey, level);

        // Create final token: info + delimiter + signature
        String token = timestampInfo + "|" + Base64.getEncoder().encodeToString(signature);
        return token.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verify a timestamp token.
     *
     * @param timestampToken The timestamp token bytes
     * @param originalHash   The original hash that was timestamped
     * @return true if valid
     */
    public boolean verifyTimestamp(byte[] timestampToken, byte[] originalHash) {
        try {
            initializeTsa();

            if (tsaCertificate == null) {
                log.warn("TSA certificate not available for verification");
                return false;
            }

            String tokenStr = new String(timestampToken, StandardCharsets.UTF_8);
            String[] parts = tokenStr.split("\\|");

            if (parts.length != 2) {
                log.warn("Invalid timestamp token format");
                return false;
            }

            String timestampInfo = parts[0];
            byte[] signature = Base64.getDecoder().decode(parts[1]);

            // Verify the hashedMessage in the token matches original
            // Parse JSON manually (simple approach)
            String hashedMessageB64 = extractJsonValue(timestampInfo, "hashedMessage");
            byte[] tokenHash = Base64.getDecoder().decode(hashedMessageB64);

            if (!MessageDigest.isEqual(tokenHash, originalHash)) {
                log.warn("Timestamp hash does not match original");
                return false;
            }

            // Verify signature
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] infoHash = digest.digest(timestampInfo.getBytes(StandardCharsets.UTF_8));

            PqcCryptoService.MlDsaLevel level = getMlDsaLevelFromCert(tsaCertificate);
            return pqcService.verify(infoHash, signature, tsaCertificate.getPublicKey(), level);

        } catch (Exception e) {
            log.error("Failed to verify timestamp", e);
            return false;
        }
    }

    /**
     * Simple JSON value extractor.
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start < 0)
            return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    /**
     * Initialize TSA by loading the TSA certificate and private key.
     */
    private synchronized void initializeTsa() {
        if (tsaInitialized)
            return;

        try {
            // Look for a CA named "TSA" or containing "Timestamp"
            var tsaCa = caRepository.findByName("TSA")
                    .or(() -> caRepository.findAll().stream()
                            .filter(ca -> ca.getName().contains("TSA") ||
                                    ca.getName().contains("Timestamp"))
                            .findFirst());

            if (tsaCa.isPresent()) {
                CertificateAuthority ca = tsaCa.get();
                if (ca.getCertificate() != null && !ca.getCertificate().isEmpty()) {
                    tsaCertificate = pqcService.parseCertificatePem(ca.getCertificate());
                }
                if (ca.getPrivateKeyPath() != null && !ca.getPrivateKeyPath().isEmpty()) {
                    tsaPrivateKey = pqcService.parsePrivateKeyPem(ca.getPrivateKeyPath());
                }
                if (tsaCertificate != null) {
                    log.info("TSA initialized with certificate: {}", tsaCertificate.getSubjectX500Principal());
                }
            } else {
                log.warn("No TSA CA found in the system. Timestamps will not be available.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize TSA", e);
        }

        tsaInitialized = true;
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
        } else {
            return PqcCryptoService.MlDsaLevel.ML_DSA_87;
        }
    }
}
