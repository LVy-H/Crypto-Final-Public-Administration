package com.gov.crypto.signaturecore.controller;

import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.signaturecore.dto.SignRequest;
import com.gov.crypto.signaturecore.dto.SignResponse;
import com.gov.crypto.signaturecore.service.UserKeyStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.security.PrivateKey;
import java.util.Base64;

/**
 * Controller for signing operations.
 * 
 * SECURITY: Uses UserKeyStorageService to retrieve stored keys.
 * Keys are associated with userId and keyAlias.
 */
@RestController
@RequestMapping("/api/v1/sign")
public class SigningController {

    private static final Logger log = LoggerFactory.getLogger(SigningController.class);

    private final PqcCryptoService pqcCryptoService;
    private final UserKeyStorageService keyStorageService;

    public SigningController(PqcCryptoService pqcCryptoService, UserKeyStorageService keyStorageService) {
        this.pqcCryptoService = pqcCryptoService;
        this.keyStorageService = keyStorageService;
    }

    /**
     * Sign data using stored key associated with userId and keyAlias.
     * 
     * @param request Contains userId, keyAlias, and base64-encoded data
     * @return Signature and algorithm used
     */
    @PostMapping("/remote")
    public SignResponse signRemote(@RequestBody SignRequest request) {
        try {
            log.info("Sign request: userId={}, keyAlias={}", request.userId(), request.keyAlias());

            // Retrieve stored private key (or default if not found)
            PrivateKey privateKey = keyStorageService.getPrivateKey(request.userId(), request.keyAlias());
            String algorithm = keyStorageService.getKeyAlgorithm(request.userId(), request.keyAlias());

            // Determine ML-DSA level from algorithm
            PqcCryptoService.MlDsaLevel level = switch (algorithm) {
                case "ML-DSA-65", "ML_DSA_65" -> PqcCryptoService.MlDsaLevel.ML_DSA_65;
                case "ML-DSA-87", "ML_DSA_87" -> PqcCryptoService.MlDsaLevel.ML_DSA_87;
                default -> PqcCryptoService.MlDsaLevel.ML_DSA_44;
            };

            byte[] dataToSign = Base64.getDecoder().decode(request.dataBase64());
            byte[] signature = pqcCryptoService.sign(dataToSign, privateKey, level);

            log.info("Successfully signed {} bytes with {}", dataToSign.length, algorithm);
            return new SignResponse(Base64.getEncoder().encodeToString(signature), algorithm);
        } catch (Exception e) {
            log.error("Signing failed", e);
            throw new RuntimeException("Signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a new key pair for a user.
     */
    @PostMapping("/generate-key")
    public KeyGenerationResponse generateKey(@RequestBody KeyGenerationRequest request) {
        try {
            PqcCryptoService.MlDsaLevel level = switch (request.algorithm()) {
                case "ML-DSA-65" -> PqcCryptoService.MlDsaLevel.ML_DSA_65;
                case "ML-DSA-87" -> PqcCryptoService.MlDsaLevel.ML_DSA_87;
                default -> PqcCryptoService.MlDsaLevel.ML_DSA_44;
            };

            var storedKey = keyStorageService.generateKeyForUser(request.userId(), request.keyAlias(), level);

            return new KeyGenerationResponse(
                    storedKey.alias(),
                    storedKey.algorithm(),
                    Base64.getEncoder().encodeToString(storedKey.publicKey().getEncoded()));
        } catch (Exception e) {
            log.error("Key generation failed", e);
            throw new RuntimeException("Key generation failed: " + e.getMessage(), e);
        }
    }

    public record KeyGenerationRequest(String userId, String keyAlias, String algorithm) {
    }

    public record KeyGenerationResponse(String alias, String algorithm, String publicKeyBase64) {
    }
}
