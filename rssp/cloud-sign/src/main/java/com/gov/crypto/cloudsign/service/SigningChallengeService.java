package com.gov.crypto.cloudsign.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing signing challenges (Signature Activation Protocol).
 * 
 * Implements the "Sole Control" requirement per Decree 23/2025:
 * - User must authorize each signing operation with OTP
 * - Challenge is bound to specific document hash
 * - Challenge expires after 5 minutes
 */
@Service
public class SigningChallengeService {

    private static final int OTP_LENGTH = 6;
    private static final long CHALLENGE_TTL_SECONDS = 300; // 5 minutes

    // In production, use Redis for distributed storage
    private final Map<String, SigningChallenge> pendingChallenges = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Represents a pending signing challenge.
     */
    public record SigningChallenge(
            String challengeId,
            String username,
            String keyAlias,
            String documentHash,
            String algorithm,
            String otp,
            Instant createdAt,
            Instant expiresAt) {

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Create a new signing challenge.
     * Returns the challenge ID and OTP (OTP should be sent to user via SMS/Email).
     */
    public ChallengeCreatedResult createChallenge(
            String username,
            String keyAlias,
            String documentHash,
            String algorithm) {

        String challengeId = generateChallengeId();
        String otp = generateOtp();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(CHALLENGE_TTL_SECONDS);

        SigningChallenge challenge = new SigningChallenge(
                challengeId,
                username,
                keyAlias,
                documentHash,
                algorithm,
                otp,
                now,
                expiresAt);

        pendingChallenges.put(challengeId, challenge);

        // Clean up expired challenges periodically
        cleanupExpiredChallenges();

        return new ChallengeCreatedResult(challengeId, otp, expiresAt);
    }

    public record ChallengeCreatedResult(String challengeId, String otp, Instant expiresAt) {
    }

    /**
     * Verify OTP and return the challenge if valid.
     * Challenge is removed after successful verification (one-time use).
     */
    public VerificationResult verifyChallenge(String challengeId, String providedOtp) {
        SigningChallenge challenge = pendingChallenges.get(challengeId);

        if (challenge == null) {
            return VerificationResult.failure("Challenge not found or already used");
        }

        if (challenge.isExpired()) {
            pendingChallenges.remove(challengeId);
            return VerificationResult.failure("Challenge expired");
        }

        if (!challenge.otp().equals(providedOtp)) {
            return VerificationResult.failure("Invalid OTP");
        }

        // Remove challenge after successful verification (one-time use)
        pendingChallenges.remove(challengeId);

        return VerificationResult.success(challenge);
    }

    public record VerificationResult(
            boolean valid,
            String errorMessage,
            SigningChallenge challenge) {

        public static VerificationResult success(SigningChallenge challenge) {
            return new VerificationResult(true, null, challenge);
        }

        public static VerificationResult failure(String message) {
            return new VerificationResult(false, message, null);
        }
    }

    /**
     * Cancel a pending challenge.
     */
    public void cancelChallenge(String challengeId) {
        pendingChallenges.remove(challengeId);
    }

    private String generateChallengeId() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    private void cleanupExpiredChallenges() {
        pendingChallenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
