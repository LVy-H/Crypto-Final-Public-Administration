package com.gov.crypto.identityservice.service;

import com.gov.crypto.model.BlacklistedToken;
import com.gov.crypto.repository.BlacklistedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for managing blacklisted JWT tokens.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public TokenBlacklistService(BlacklistedTokenRepository blacklistedTokenRepository) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    /**
     * Check if a token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        String tokenHash = hashToken(token);
        return blacklistedTokenRepository.existsByTokenHash(tokenHash);
    }

    /**
     * Blacklist a token (e.g., on logout).
     */
    @Transactional
    public void blacklistToken(String token, UUID userId, Instant expiresAt, BlacklistedToken.BlacklistReason reason) {
        String tokenHash = hashToken(token);

        // Check if already blacklisted
        if (blacklistedTokenRepository.existsByTokenHash(tokenHash)) {
            log.debug("Token already blacklisted");
            return;
        }

        BlacklistedToken blacklistedToken = new BlacklistedToken(tokenHash, userId, expiresAt, reason);
        blacklistedTokenRepository.save(blacklistedToken);
        log.info("Token blacklisted for user {} with reason {}", userId, reason);
    }

    /**
     * Hash a token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Cleanup expired tokens (runs every hour).
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = blacklistedTokenRepository.deleteExpiredTokens(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired blacklisted tokens", deleted);
        }
    }
}
