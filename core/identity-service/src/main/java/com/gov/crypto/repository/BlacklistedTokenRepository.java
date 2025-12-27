package com.gov.crypto.repository;

import com.gov.crypto.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {

    /**
     * Check if a token hash exists in the blacklist
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * Find a blacklisted token by its hash
     */
    Optional<BlacklistedToken> findByTokenHash(String tokenHash);

    /**
     * Delete expired tokens (for cleanup job)
     */
    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Blacklist all tokens for a user (e.g., on password change)
     */
    @Query("SELECT COUNT(b) FROM BlacklistedToken b WHERE b.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
