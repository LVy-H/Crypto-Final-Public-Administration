package com.gov.crypto.cloudsign.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Security utilities for input validation and sanitization.
 * Addresses OWASP Top 10 vulnerabilities for cryptographic applications.
 */
public final class SecurityUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Patterns for input validation
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.]{1,64}$");
    private static final Pattern SAFE_ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final Pattern SAFE_EMAIL_PATTERN = Pattern
            .compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    // Enterprise Security Policy: Only post-quantum algorithms with NIST Level 3+
    // are allowed
    // ML-DSA-65 = NIST Level 3, ML-DSA-87 = NIST Level 5
    // ML-DSA-44, RSA, ECDSA, Ed25519 are deprecated per enterprise security policy
    private static final Pattern SAFE_ALGORITHM_PATTERN = Pattern.compile(
            "^(mldsa65|mldsa87|ML-DSA-65|ML-DSA-87)$", Pattern.CASE_INSENSITIVE);

    // Dangerous characters for command injection
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[`$;|&<>(){}\\[\\]!#*?~]");

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Validates and sanitizes a Distinguished Name (DN) component.
     * Prevents command injection via OpenSSL -subj parameter.
     */
    public static String sanitizeDnComponent(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        if (DANGEROUS_CHARS.matcher(input).find()) {
            throw new IllegalArgumentException(fieldName + " contains dangerous characters");
        }

        if (!SAFE_NAME_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }

        return input.replace("\\", "\\\\").replace(",", "\\,")
                .replace("+", "\\+").replace("\"", "\\\"")
                .replace("<", "\\<").replace(">", "\\>");
    }

    /**
     * Validates and sanitizes a key alias. Prevents path traversal attacks.
     */
    public static String sanitizeKeyAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Key alias cannot be null or empty");
        }

        if (alias.contains("..") || alias.contains("/") || alias.contains("\\")) {
            throw new IllegalArgumentException("Key alias contains path traversal characters");
        }

        if (!SAFE_ALIAS_PATTERN.matcher(alias).matches()) {
            throw new IllegalArgumentException("Key alias must be alphanumeric with optional hyphens/underscores");
        }

        return alias;
    }

    /**
     * Validates an algorithm name.
     */
    public static String validateAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("Algorithm cannot be null or empty");
        }

        if (!SAFE_ALGORITHM_PATTERN.matcher(algorithm).matches()) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }

        return algorithm.toLowerCase();
    }

    /**
     * Validates Base64-encoded data.
     */
    public static String validateBase64(String base64Data, String fieldName, int maxDecodedSize) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(base64Data);
            if (decoded.length > maxDecodedSize) {
                throw new IllegalArgumentException(fieldName + " exceeds maximum size");
            }
            return base64Data;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " is not valid Base64", e);
        }
    }

    /**
     * Generates a cryptographically secure serial number for certificates.
     */
    public static String generateSecureSerialNumber() {
        byte[] serialBytes = new byte[20];
        SECURE_RANDOM.nextBytes(serialBytes);
        serialBytes[0] &= 0x7F;
        return new BigInteger(1, serialBytes).toString(16).toUpperCase();
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    public static boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates Base64-encoded data and returns decoded bytes.
     */
    public static byte[] validateBase64Bytes(String base64Data, String fieldName, int maxDecodedSize) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(base64Data);
            if (decoded.length > maxDecodedSize) {
                throw new IllegalArgumentException(fieldName + " exceeds maximum size of " + maxDecodedSize + " bytes");
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " is not valid Base64", e);
        }
    }

    /**
     * Validates a file path is within an allowed base directory.
     * Returns a Path object for safer file operations.
     */
    public static java.nio.file.Path validatePathWithinBase(String basePath, String filename) {
        try {
            java.nio.file.Path base = java.nio.file.Path.of(basePath).toAbsolutePath().normalize();
            java.nio.file.Path requested = base.resolve(filename).normalize();

            if (!requested.startsWith(base)) {
                throw new IllegalArgumentException("Path traversal detected: " + filename);
            }

            return requested;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid path: " + filename, e);
        }
    }

    /**
     * Sanitizes an entire subject DN string.
     * Validates each component and escapes dangerous characters.
     */
    public static String sanitizeSubjectDn(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject DN cannot be null or empty");
        }

        if (DANGEROUS_CHARS.matcher(subject).find()) {
            throw new IllegalArgumentException("Subject DN contains dangerous characters");
        }

        // Basic validation - must start with / and contain at least CN
        if (!subject.startsWith("/") || !subject.contains("CN=")) {
            throw new IllegalArgumentException("Invalid subject DN format. Must start with / and contain CN=");
        }

        // Escape special characters that could cause issues
        return subject.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
