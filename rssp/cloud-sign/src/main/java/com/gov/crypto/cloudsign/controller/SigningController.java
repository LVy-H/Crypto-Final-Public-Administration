package com.gov.crypto.cloudsign.controller;

import com.gov.crypto.cloudsign.security.SadValidator;
import com.gov.crypto.cloudsign.security.SadValidator.ValidationResult;
import com.gov.crypto.cloudsign.service.SigningChallengeService;
import com.gov.crypto.cloudsign.service.SigningChallengeService.ChallengeCreatedResult;
import com.gov.crypto.cloudsign.service.SigningChallengeService.SigningChallenge;
import com.gov.crypto.cloudsign.service.SigningChallengeService.VerificationResult;
import com.gov.crypto.service.KeyStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;

/**
 * Cloud Signing Controller implementing CSC (Cloud Signature Consortium) API.
 * 
 * SECURITY: Implements Signature Activation Protocol (SAP) per Decree 23/2025.
 * All signing operations require two-step verification:
 * 1. /sign/init - Create challenge with OTP
 * 2. /sign/confirm - Verify OTP and execute signing
 * 
 * This ensures "Sole Control" - the user, not the server, authorizes each
 * signature.
 */
@RestController
@RequestMapping("/csc/v1")
public class SigningController {

    private static final Logger log = LoggerFactory.getLogger(SigningController.class);

    private final KeyStorageService keyStorageService;
    private final SadValidator sadValidator;
    private final SigningChallengeService signingChallengeService;

    public SigningController(
            KeyStorageService keyStorageService,
            SadValidator sadValidator,
            SigningChallengeService signingChallengeService) {
        this.keyStorageService = keyStorageService;
        this.sadValidator = sadValidator;
        this.signingChallengeService = signingChallengeService;
    }

    // ============ Request/Response Records ============

    record SignInitRequest(String keyAlias, String dataHashBase64, String algorithm) {
    }

    record SignInitResponse(
            String challengeId,
            String documentHash,
            Instant expiresAt,
            String message) {
    }

    record SignConfirmRequest(String challengeId, String otp) {
    }

    record SignConfirmResponse(String signatureBase64, String keyAlias, String algorithm) {
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

    record CsrRequest(String alias, String subject) {
    }

    record CsrResponse(String csrPem) {
    }

    // ============ Signing Endpoints (SAP Two-Step Flow) ============

    /**
     * Step 1: Initialize signing request.
     * 
     * Creates a signing challenge and generates OTP.
     * In production, OTP should be sent to user via SMS/Email.
     * 
     * SECURITY: Validates session token before creating challenge.
     */
    @PostMapping("/sign/init")
    public ResponseEntity<?> initializeSigning(
            @RequestBody SignInitRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Principal principal) {

        // Validate session token (not OTP, just authentication)
        ValidationResult validation = sadValidator.validate(authHeader, request.keyAlias(), principal);

        if (!validation.valid()) {
            log.warn("Sign init rejected: {} for key {}", validation.errorMessage(), request.keyAlias());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(validation.errorMessage(), "UNAUTHORIZED"));
        }

        try {
            log.info("Creating signing challenge for user {} on key {}",
                    validation.username(), request.keyAlias());

            ChallengeCreatedResult challenge = signingChallengeService.createChallenge(
                    validation.username(),
                    request.keyAlias(),
                    request.dataHashBase64(),
                    request.algorithm());

            // In production, send OTP via SMS/Email here or rely on TOTP app
            log.info("Challenge created: {}", challenge.challengeId());

            return ResponseEntity.ok(new SignInitResponse(
                    challenge.challengeId(),
                    request.dataHashBase64(),
                    challenge.expiresAt(),
                    "Challenge created. Enter TOTP from Authenticator App to confirm signing."));

        } catch (Exception e) {
            log.error("Failed to create signing challenge: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("Failed to initialize signing", "INTERNAL_ERROR"));
        }
    }

    /**
     * Step 2: Confirm signing with OTP.
     * 
     * Verifies the OTP matches the challenge and executes the signature.
     * Challenge is consumed after use (one-time).
     * 
     * SECURITY: OTP verification ensures user explicitly authorized this specific
     * signature.
     */
    @PostMapping("/sign/confirm")
    public ResponseEntity<?> confirmSigning(@RequestBody SignConfirmRequest request) {

        if (request.challengeId() == null || request.otp() == null) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("Missing challengeId or otp", "INVALID_REQUEST"));
        }

        // Verify OTP and get challenge details
        VerificationResult verification = signingChallengeService.verifyChallenge(
                request.challengeId(), request.otp());

        if (!verification.valid()) {
            log.warn("OTP verification failed for challenge {}: {}",
                    request.challengeId(), verification.errorMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(verification.errorMessage(), "OTP_INVALID"));
        }

        SigningChallenge challenge = verification.challenge();

        try {
            log.info("OTP verified. Executing signature for user {} on key {}",
                    challenge.username(), challenge.keyAlias());

            // Execute the actual signing operation
            String signature = keyStorageService.signHash(
                    challenge.keyAlias(),
                    challenge.documentHash(),
                    challenge.algorithm());

            log.info("Signature created successfully for user {} on key {}",
                    challenge.username(), challenge.keyAlias());

            return ResponseEntity.ok(new SignConfirmResponse(
                    signature,
                    challenge.keyAlias(),
                    challenge.algorithm()));

        } catch (Exception e) {
            log.error("Signing failed for user {} on key {}: {}",
                    challenge.username(), challenge.keyAlias(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("Signing operation failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Cancel a pending signing challenge.
     */
    @DeleteMapping("/sign/cancel/{challengeId}")
    public ResponseEntity<?> cancelSigning(
            @PathVariable String challengeId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Basic auth check (in production, verify user owns the challenge)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("Missing authorization", "UNAUTHORIZED"));
        }

        signingChallengeService.cancelChallenge(challengeId);
        log.info("Challenge {} cancelled", challengeId);

        return ResponseEntity.ok().body("{\"status\": \"cancelled\"}");
    }

    // ============ Legacy Direct Sign (DEPRECATED) ============

    /**
     * @deprecated Use /sign/init and /sign/confirm instead.
     *             This endpoint is retained for backward compatibility but should
     *             be removed in production.
     */
    @Deprecated
    record SignRequest(String keyAlias, String dataHashBase64, String algorithm) {
    }

    @Deprecated
    record SignResponse(String signatureBase64) {
    }

    @Deprecated
    @PostMapping("/sign")
    public ResponseEntity<?> signHash(
            @RequestBody SignRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Principal principal) {

        log.warn("DEPRECATED: Direct /sign endpoint called. Use /sign/init and /sign/confirm for SAP compliance.");

        // Still validate SAD but warn about deprecation
        ValidationResult validation = sadValidator.validate(authHeader, request.keyAlias(), principal);

        if (!validation.valid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(validation.errorMessage(), "UNAUTHORIZED"));
        }

        try {
            String signature = keyStorageService.signHash(
                    request.keyAlias(),
                    request.dataHashBase64(),
                    request.algorithm());

            return ResponseEntity.ok(new SignResponse(signature));

        } catch (Exception e) {
            log.error("Signing failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("Signing operation failed", "INTERNAL_ERROR"));
        }
    }

    // ============ Key Management Endpoints ============

    /**
     * Generate a new key pair.
     */
    @PostMapping("/keys/generate")
    public ResponseEntity<?> generateKey(
            @RequestBody KeyGenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Principal principal) {

        ValidationResult validation = sadValidator.validate(authHeader, request.alias(), principal);

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

    /**
     * Generate a Certificate Signing Request (CSR).
     */
    @PostMapping("/keys/csr")
    public ResponseEntity<?> generateCsr(
            @RequestBody CsrRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Principal principal) {

        ValidationResult validation = sadValidator.validate(authHeader, request.alias(), principal);

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
