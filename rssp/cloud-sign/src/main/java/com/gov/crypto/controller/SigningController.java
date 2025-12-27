package com.gov.crypto.cloudsign.controller;

import com.gov.crypto.cloudsign.security.SadValidator;
import com.gov.crypto.cloudsign.security.SadValidator.ValidationResult;
import com.gov.crypto.service.KeyStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Cloud Signing Controller implementing CSC (Cloud Signature Consortium) API.
 * 
 * SECURITY: All signing operations require valid SAD (Signature Activation
 * Data)
 * to prevent unauthorized signature creation.
 */
@RestController
@RequestMapping("/csc/v1")
public class SigningController {

    private static final Logger log = LoggerFactory.getLogger(SigningController.class);

    private final KeyStorageService keyStorageService;
    private final SadValidator sadValidator;

    public SigningController(KeyStorageService keyStorageService, SadValidator sadValidator) {
        this.keyStorageService = keyStorageService;
        this.sadValidator = sadValidator;
    }

    record SignRequest(String keyAlias, String dataHashBase64, String algorithm) {
    }

    record SignResponse(String signatureBase64) {
    }

    record ErrorResponse(String error, String code, Instant timestamp) {
        static ErrorResponse of(String error, String code) {
            return new ErrorResponse(error, code, Instant.now());
        }
    }

    record KeyGenRequest(String alias, String algorithm) {
    }

    record KeyGenResponse(String publicKeyPem) {
    }

    /**
     * Sign a hash using a specific key.
     * 
     * SECURITY: Requires valid Bearer token in Authorization header.
     * The token must belong to the owner of the specified key.
     */
    @PostMapping("/sign")
    public ResponseEntity<?> signHash(
            @RequestBody SignRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // SECURITY FIX: Validate SAD (Signature Activation Data) before signing
        ValidationResult validation = sadValidator.validate(authHeader, request.keyAlias());

        if (!validation.valid()) {
            log.warn("Signing request rejected: {} for key {}", validation.errorMessage(), request.keyAlias());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(validation.errorMessage(), "UNAUTHORIZED"));
        }

        try {
            // Log successful authorization for audit trail
            log.info("Signing authorized for user {} on key {}", validation.username(), request.keyAlias());

            String signature = keyStorageService.signHash(
                    request.keyAlias(),
                    request.dataHashBase64(),
                    request.algorithm());

            // Log successful signing for audit trail
            log.info("Signature created successfully for user {} on key {}",
                    validation.username(), request.keyAlias());

            return ResponseEntity.ok(new SignResponse(signature));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid signing request from {}: {}", validation.username(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("Signing failed for user {} on key {}: {}",
                    validation.username(), request.keyAlias(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("Signing operation failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Generate a new key pair.
     * 
     * SECURITY: Requires valid Bearer token. Key ownership is tied to the
     * authenticated user.
     */
    @PostMapping("/keys/generate")
    public ResponseEntity<?> generateKey(
            @RequestBody KeyGenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Validate authorization for key generation
        // For key generation, we validate against the alias being created
        ValidationResult validation = sadValidator.validate(authHeader, request.alias());

        if (!validation.valid()) {
            log.warn("Key generation rejected: {} for alias {}", validation.errorMessage(), request.alias());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(validation.errorMessage(), "UNAUTHORIZED"));
        }

        try {
            log.info("Generating key pair for user {} with alias {}", validation.username(), request.alias());

            String publicKey = keyStorageService.generateKeyPair(request.alias(), request.algorithm());

            log.info("Key pair generated successfully for user {}", validation.username());

            return ResponseEntity.ok(new KeyGenResponse(publicKey));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid key generation request from {}: {}", validation.username(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("Key generation failed for user {}: {}", validation.username(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("Key generation failed", "INTERNAL_ERROR"));
        }
    }

    record CsrRequest(String alias, String subject) {
    }

    record CsrResponse(String csrPem) {
    }

    /**
     * Generate a Certificate Signing Request (CSR).
     * 
     * SECURITY: Requires valid Bearer token. Only the key owner can generate CSRs.
     */
    @PostMapping("/keys/csr")
    public ResponseEntity<?> generateCsr(
            @RequestBody CsrRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        ValidationResult validation = sadValidator.validate(authHeader, request.alias());

        if (!validation.valid()) {
            log.warn("CSR generation rejected: {} for alias {}", validation.errorMessage(), request.alias());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(validation.errorMessage(), "UNAUTHORIZED"));
        }

        try {
            log.info("Generating CSR for user {} with alias {}", validation.username(), request.alias());

            String csr = keyStorageService.generateCsr(request.alias(), request.subject());

            return ResponseEntity.ok(new CsrResponse(csr));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid CSR request from {}: {}", validation.username(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("CSR generation failed for user {}: {}", validation.username(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("CSR generation failed", "INTERNAL_ERROR"));
        }
    }
}
