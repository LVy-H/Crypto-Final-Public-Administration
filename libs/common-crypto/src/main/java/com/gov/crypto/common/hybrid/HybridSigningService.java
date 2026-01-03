package com.gov.crypto.common.hybrid;

import com.gov.crypto.common.ecdsa.StandardCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService.MlDsaLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Hybrid Signing Service combining ECDSA (primary) + Dilithium (secondary).
 * 
 * @deprecated Use {@link com.gov.crypto.common.pqc.PqcCryptoService} for pure
 *             ML-DSA operations.
 *             Hybrid signatures are deprecated in pure PQC architecture. Use
 *             ML-DSA directly.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("removal") // Uses StandardCryptoService which is also deprecated
public class HybridSigningService {

    private static final Logger log = LoggerFactory.getLogger(HybridSigningService.class);

    private final StandardCryptoService ecdsaService;
    private final PqcCryptoService pqcService;
    private final MlDsaLevel dilithiumLevel;

    public HybridSigningService() {
        this(MlDsaLevel.ML_DSA_65); // Default: NIST Level 3
    }

    public HybridSigningService(MlDsaLevel dilithiumLevel) {
        this.ecdsaService = new StandardCryptoService();
        this.pqcService = new PqcCryptoService();
        this.dilithiumLevel = dilithiumLevel;
    }

    /**
     * Represents a hybrid key pair (ECDSA + Dilithium).
     */
    public record HybridKeyPair(
            KeyPair ecdsaKeyPair,
            KeyPair dilithiumKeyPair) {

        public PublicKey ecdsaPublicKey() {
            return ecdsaKeyPair.getPublic();
        }

        public PrivateKey ecdsaPrivateKey() {
            return ecdsaKeyPair.getPrivate();
        }

        public PublicKey dilithiumPublicKey() {
            return dilithiumKeyPair.getPublic();
        }

        public PrivateKey dilithiumPrivateKey() {
            return dilithiumKeyPair.getPrivate();
        }
    }

    /**
     * Represents a hybrid signature (ECDSA primary + Dilithium secondary).
     */
    public record HybridSignature(
            byte[] ecdsaSignature,
            byte[] dilithiumSignature) {

        /**
         * Get Base64-encoded ECDSA signature for PDF embedding.
         */
        public String ecdsaBase64() {
            return Base64.getEncoder().encodeToString(ecdsaSignature);
        }

        /**
         * Get Base64-encoded Dilithium signature for unsigned attribute.
         */
        public String dilithiumBase64() {
            return Base64.getEncoder().encodeToString(dilithiumSignature);
        }

        /**
         * Combined signature as JSON for storage/transmission.
         */
        public String toJson() {
            return String.format(
                    "{\"ecdsa\":\"%s\",\"dilithium\":\"%s\",\"type\":\"hybrid-ecdsa-dilithium\"}",
                    ecdsaBase64(), dilithiumBase64());
        }
    }

    /**
     * Generate hybrid key pair (both ECDSA and Dilithium).
     */
    public HybridKeyPair generateHybridKeyPair() throws Exception {
        log.info("Generating hybrid key pair (ECDSA P-384 + {})", dilithiumLevel);

        KeyPair ecdsaKp = ecdsaService.generateEcdsaKeyPair();
        KeyPair dilithiumKp = pqcService.generateMlDsaKeyPair(dilithiumLevel);

        log.info("Hybrid key pair generated successfully");
        return new HybridKeyPair(ecdsaKp, dilithiumKp);
    }

    /**
     * Sign data with hybrid approach.
     * 
     * @param data          The data to sign (typically document hash)
     * @param hybridKeyPair The hybrid key pair
     * @return HybridSignature containing both signatures
     */
    public HybridSignature sign(byte[] data, HybridKeyPair hybridKeyPair) throws Exception {
        log.debug("Creating hybrid signature");

        // Primary signature (ECDSA) - this goes in the PDF's signature field
        byte[] ecdsaSig = ecdsaService.sign(data, hybridKeyPair.ecdsaPrivateKey());

        // Secondary signature (Dilithium) - this goes in unsigned attribute
        byte[] dilithiumSig = pqcService.sign(data, hybridKeyPair.dilithiumPrivateKey(), dilithiumLevel);

        log.debug("Hybrid signature created: ECDSA ({} bytes) + Dilithium ({} bytes)",
                ecdsaSig.length, dilithiumSig.length);

        return new HybridSignature(ecdsaSig, dilithiumSig);
    }

    /**
     * Verify hybrid signature.
     * 
     * @param data          Original data
     * @param signature     Hybrid signature to verify
     * @param hybridKeyPair Hybrid key pair (public keys)
     * @return VerificationResult with detailed status
     */
    public VerificationResult verify(byte[] data, HybridSignature signature, HybridKeyPair hybridKeyPair)
            throws Exception {
        log.debug("Verifying hybrid signature");

        boolean ecdsaValid = ecdsaService.verify(data, signature.ecdsaSignature(),
                hybridKeyPair.ecdsaPublicKey());

        boolean dilithiumValid = pqcService.verify(data, signature.dilithiumSignature(),
                hybridKeyPair.dilithiumPublicKey(), dilithiumLevel);

        log.debug("Verification result: ECDSA={}, Dilithium={}", ecdsaValid, dilithiumValid);

        return new VerificationResult(ecdsaValid, dilithiumValid);
    }

    /**
     * Verification result with both signature statuses.
     */
    public record VerificationResult(
            boolean ecdsaValid,
            boolean dilithiumValid) {

        /**
         * Check if at least the primary (ECDSA) signature is valid.
         * This is sufficient for current PDF viewers.
         */
        public boolean isPrimaryValid() {
            return ecdsaValid;
        }

        /**
         * Check if both signatures are valid (full hybrid verification).
         */
        public boolean isFullyValid() {
            return ecdsaValid && dilithiumValid;
        }

        /**
         * Check if document is post-quantum secure.
         */
        public boolean isPqcSecure() {
            return dilithiumValid;
        }
    }

    // Accessors for individual services if needed
    public StandardCryptoService getEcdsaService() {
        return ecdsaService;
    }

    public PqcCryptoService getPqcService() {
        return pqcService;
    }

    public MlDsaLevel getDilithiumLevel() {
        return dilithiumLevel;
    }
}
