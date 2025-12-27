package com.gov.crypto.common.security;

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
     *
     * @param input     the DN component to validate
     * @param fieldName field name for error messages
     * @return sanitized input
     * @throws IllegalArgumentException if input is invalid
     */
    public static String sanitizeDnComponent(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        // Check for command injection attempts
        if (DANGEROUS_CHARS.matcher(input).find()) {
            throw new IllegalArgumentException(fieldName + " contains dangerous characters");
        }

        // Validate against safe pattern
        if (!SAFE_NAME_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException(fieldName
                    + " contains invalid characters. Only alphanumeric, spaces, hyphens, underscores, and dots are allowed.");
        }

        // Escape special characters for OpenSSL DN format
        return input
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace("+", "\\+")
                .replace("\"", "\\\"")
                .replace("<", "\\<")
                .replace(">", "\\>");
    }

    /**
     * Validates and sanitizes a key alias.
     * Prevents path traversal attacks.
     *
     * @param alias the key alias to validate
     * @return validated alias
     * @throws IllegalArgumentException if alias is invalid
     */
    public static String sanitizeKeyAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Key alias cannot be null or empty");
        }

        // Check for path traversal
        if (alias.contains("..") || alias.contains("/") || alias.contains("\\")) {
            throw new IllegalArgumentException("Key alias contains path traversal characters");
        }

        // Validate against safe pattern
        if (!SAFE_ALIAS_PATTERN.matcher(alias).matches()) {
            throw new IllegalArgumentException(
                    "Key alias must be alphanumeric with optional hyphens/underscores (1-64 chars)");
        }

        return alias;
    }

    /**
     * Validates an algorithm name.
     *
     * @param algorithm the algorithm to validate
     * @return validated algorithm
     * @throws IllegalArgumentException if algorithm is not supported
     */
    public static String validateAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("Algorithm cannot be null or empty");
        }

        if (!SAFE_ALGORITHM_PATTERN.matcher(algorithm).matches()) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm +
                    ". Enterprise policy requires ML-DSA-65 (NIST Level 3) or ML-DSA-87 (NIST Level 5)");
        }

        return algorithm.toLowerCase();
    }

    /**
     * Validates an email address.
     *
     * @param email the email to validate
     * @return validated email
     * @throws IllegalArgumentException if email is invalid
     */
    public static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        // Check for command injection
        if (DANGEROUS_CHARS.matcher(email).find()) {
            throw new IllegalArgumentException("Email contains dangerous characters");
        }

        if (!SAFE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        return email.toLowerCase();
    }

    /**
     * Generates a cryptographically secure serial number for certificates.
     * Compliant with RFC 5280 (20 bytes / 160 bits).
     *
     * @return hex-encoded serial number
     */
    public static String generateSecureSerialNumber() {
        byte[] serialBytes = new byte[20]; // 160 bits per RFC 5280
        SECURE_RANDOM.nextBytes(serialBytes);
        // Ensure positive by clearing MSB
        serialBytes[0] &= 0x7F;
        return new BigInteger(1, serialBytes).toString(16).toUpperCase();
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * @param a first string
     * @param b second string
     * @return true if strings are equal
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
     * Validates a file path is within an allowed base directory.
     * Prevents path traversal attacks.
     *
     * @param basePath      the allowed base directory
     * @param requestedPath the path to validate
     * @return the validated canonical path
     * @throws IllegalArgumentException if path is outside base directory
     */
    public static String validatePathWithinBase(String basePath, String requestedPath) {
        try {
            java.nio.file.Path base = java.nio.file.Path.of(basePath).toRealPath();
            java.nio.file.Path requested = java.nio.file.Path.of(requestedPath).normalize();

            // If the requested path is relative, resolve it against base
            if (!requested.isAbsolute()) {
                requested = base.resolve(requested).normalize();
            }

            // Verify the path is within the base directory
            if (!requested.startsWith(base)) {
                throw new IllegalArgumentException("Path traversal detected: " + requestedPath);
            }

            return requested.toString();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid path: " + requestedPath, e);
        }
    }

    /**
     * Validates Base64-encoded data.
     *
     * @param base64Data     the Base64 string to validate
     * @param fieldName      field name for error messages
     * @param maxDecodedSize maximum allowed decoded size in bytes
     * @return validated Base64 string
     * @throws IllegalArgumentException if invalid
     */
    public static String validateBase64(String base64Data, String fieldName, int maxDecodedSize) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(base64Data);
            if (decoded.length > maxDecodedSize) {
                throw new IllegalArgumentException(fieldName + " exceeds maximum size of " + maxDecodedSize + " bytes");
            }
            return base64Data;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " is not valid Base64", e);
        }
    }

    /**
     * Validates a PEM-formatted certificate or key.
     *
     * @param pemData      the PEM data to validate
     * @param expectedType expected type (e.g., "CERTIFICATE", "PRIVATE KEY")
     * @return validated PEM string
     * @throws IllegalArgumentException if invalid
     */
    public static String validatePem(String pemData, String expectedType) {
        if (pemData == null || pemData.isBlank()) {
            throw new IllegalArgumentException("PEM data cannot be null or empty");
        }

        String header = "-----BEGIN " + expectedType + "-----";
        String footer = "-----END " + expectedType + "-----";

        if (!pemData.contains(header) || !pemData.contains(footer)) {
            throw new IllegalArgumentException("Invalid PEM format. Expected type: " + expectedType);
        }

        // Check for command injection in PEM
        if (DANGEROUS_CHARS.matcher(pemData.replace("-", "").replace("\n", "").replace(" ", "")).find()) {
            throw new IllegalArgumentException("PEM data contains suspicious characters");
        }

        return pemData;
    }
}
