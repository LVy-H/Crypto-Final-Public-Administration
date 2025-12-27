package com.gov.crypto.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity to store blacklisted JWT tokens.
 * Tokens are blacklisted on user logout or security revocation.
 */
@Entity
@Table(name = "blacklisted_tokens", indexes = {
        @Index(name = "idx_token_hash", columnList = "tokenHash"),
        @Index(name = "idx_expires_at", columnList = "expiresAt")
})
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * SHA-256 hash of the token (not the token itself for security)
     */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * User who owned this token
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * When this token expires (for cleanup)
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * When the token was blacklisted
     */
    @Column(nullable = false)
    private Instant blacklistedAt;

    /**
     * Reason for blacklisting
     */
    @Enumerated(EnumType.STRING)
    private BlacklistReason reason;

    public enum BlacklistReason {
        LOGOUT,
        SECURITY_REVOCATION,
        PASSWORD_CHANGE,
        ADMIN_ACTION
    }

    // Constructors
    public BlacklistedToken() {
    }

    public BlacklistedToken(String tokenHash, UUID userId, Instant expiresAt, BlacklistReason reason) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.blacklistedAt = Instant.now();
        this.reason = reason;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Instant blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public BlacklistReason getReason() {
        return reason;
    }

    public void setReason(BlacklistReason reason) {
        this.reason = reason;
    }
}
