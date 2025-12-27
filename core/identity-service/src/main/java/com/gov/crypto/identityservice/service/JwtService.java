package com.gov.crypto.identityservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // JWT secret key - MUST be set via environment variable in production
    // If not set, a random key will be generated (only suitable for development)
    @Value("${jwt.secret:}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            // Generate random 256-bit key for development only
            System.err.println(
                    "WARNING: jwt.secret not set! Generating random key. This is NOT suitable for production!");
            System.err.println("Set JWT_SECRET environment variable or jwt.secret property for production use.");
            byte[] randomKey = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(randomKey);
            secret = Base64.getEncoder().encodeToString(randomKey);
        }

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) for HS256. " +
                    "Current key is " + (keyBytes.length * 8) + " bits. Set jwt.secret property.");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String username, com.gov.crypto.model.Role role, String identityStatus) {
        Map<String, Object> claims = new HashMap<>();
        if (role != null) {
            claims.put("role", role.getName());
            claims.put("permissions", role.getPermissions().stream()
                    .map(com.gov.crypto.model.Permission::getName)
                    .collect(java.util.stream.Collectors.toList()));
        } else {
            claims.put("role", "USER"); // Fallback
            claims.put("permissions", java.util.Collections.emptyList());
        }
        claims.put("identity_status", identityStatus);
        return createToken(claims, username);
    }

    public String generateIdentityAssertion(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "IDENTITY_ASSERTION");
        claims.put("status", "VERIFIED");
        claims.put("verified_at", System.currentTimeMillis());
        // Longer expiration for assertion (e.g. 1 year) or indefinite?
        // Let's set 90 days
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 90))
                .signWith(signingKey)
                .compact();
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates a JWT token using constant-time comparison to prevent timing
     * attacks.
     */
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        // SECURITY: Use constant-time comparison to prevent timing attacks
        boolean usernameMatches = MessageDigest.isEqual(
                extractedUsername.getBytes(StandardCharsets.UTF_8),
                username.getBytes(StandardCharsets.UTF_8));
        return usernameMatches && !isTokenExpired(token);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
