package com.gov.crypto.common.openssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Secure OpenSSL Service - Centralized wrapper for all OpenSSL operations.
 * 
 * SECURITY FEATURES:
 * - Input validation and sanitization for all parameters
 * - Path traversal prevention
 * - Command injection prevention
 * - Automatic temporary file cleanup
 * - Comprehensive audit logging
 * 
 * This class should be used by all services that need to execute OpenSSL
 * commands.
 */
public class SecureOpenSSLService {

    private static final Logger log = LoggerFactory.getLogger(SecureOpenSSLService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PROCESS_TIMEOUT_SECONDS = 30;

    // Input validation patterns
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.]{1,64}$");
    private static final Pattern SAFE_ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    // Enterprise Security Policy: Only post-quantum algorithms with NIST Level 3+
    // are allowed
    // ML-DSA-65 = NIST Level 3, ML-DSA-87 = NIST Level 5
    // ML-DSA-44, RSA, ECDSA, Ed25519 are deprecated per enterprise security policy
    private static final Pattern SAFE_ALGORITHM_PATTERN = Pattern.compile(
            "^(mldsa65|mldsa87|ML-DSA-65|ML-DSA-87)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[`$;|&<>(){}\\[\\]!#*?~]");

    private final Path keyStoragePath;
    private final Path certStoragePath;

    public SecureOpenSSLService(String keyStoragePath, String certStoragePath) {
        this.keyStoragePath = Path.of(keyStoragePath).toAbsolutePath();
        this.certStoragePath = Path.of(certStoragePath).toAbsolutePath();
        ensureDirectories();
    }

    // ============= INPUT VALIDATION =============

    /**
     * Validates and sanitizes a Distinguished Name (DN) component.
     * Prevents command injection via OpenSSL -subj parameter.
     */
    public String sanitizeDnComponent(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        if (DANGEROUS_CHARS.matcher(input).find()) {
            throw new IllegalArgumentException(fieldName + " contains dangerous characters");
        }

        if (!SAFE_NAME_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters. " +
                    "Only alphanumeric, spaces, hyphens, underscores, and dots allowed.");
        }

        return input.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace("+", "\\+")
                .replace("\"", "\\\"")
                .replace("<", "\\<")
                .replace(">", "\\>");
    }

    /**
     * Validates and sanitizes a key/file alias.
     * Prevents path traversal attacks.
     */
    public String sanitizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Alias cannot be null or empty");
        }

        if (alias.contains("..") || alias.contains("/") || alias.contains("\\")) {
            throw new IllegalArgumentException("Alias contains path traversal characters");
        }

        if (!SAFE_ALIAS_PATTERN.matcher(alias).matches()) {
            throw new IllegalArgumentException(
                    "Alias must be alphanumeric with optional hyphens/underscores (1-64 chars)");
        }

        return alias;
    }

    /**
     * Validates an algorithm name against whitelist.
     */
    public String validateAlgorithm(String algorithm) {
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
     * Validates Base64-encoded data.
     */
    public byte[] validateAndDecodeBase64(String base64Data, String fieldName, int maxDecodedSize) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(base64Data);
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
     */
    public Path validatePathWithinBase(Path basePath, String relativePath) {
        try {
            Path requested = basePath.resolve(relativePath).normalize().toAbsolutePath();

            if (!requested.startsWith(basePath.toAbsolutePath())) {
                throw new IllegalArgumentException("Path traversal detected: " + relativePath);
            }

            return requested;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid path: " + relativePath, e);
        }
    }

    /**
     * Validates PEM-formatted data.
     */
    public String validatePem(String pemData, String expectedType) {
        if (pemData == null || pemData.isBlank()) {
            throw new IllegalArgumentException("PEM data cannot be null or empty");
        }

        String header = "-----BEGIN " + expectedType + "-----";
        String footer = "-----END " + expectedType + "-----";

        if (!pemData.contains(header) || !pemData.contains(footer)) {
            throw new IllegalArgumentException("Invalid PEM format. Expected type: " + expectedType);
        }

        return pemData;
    }

    // ============= CRYPTOGRAPHIC UTILITIES =============

    /**
     * Generates a cryptographically secure serial number for certificates (RFC 5280
     * compliant).
     */
    public String generateSecureSerialNumber() {
        byte[] serialBytes = new byte[20]; // 160 bits per RFC 5280
        SECURE_RANDOM.nextBytes(serialBytes);
        serialBytes[0] &= 0x7F; // Ensure positive
        return new BigInteger(1, serialBytes).toString(16).toUpperCase();
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    public boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    // ============= OPENSSL OPERATIONS =============

    /**
     * Generates a PQC key pair and returns the public key PEM.
     */
    public String generateKeyPair(String alias, String algorithm) throws OpenSSLException {
        String sanitizedAlias = sanitizeAlias(alias);
        String validatedAlgorithm = validateAlgorithm(algorithm);
        Path keyPath = validatePathWithinBase(keyStoragePath, sanitizedAlias + ".pem");

        log.info("Generating {} key pair with alias {}", validatedAlgorithm, sanitizedAlias);

        // Generate private key
        runOpenSSL(List.of(
                "openssl", "genpkey",
                "-algorithm", validatedAlgorithm,
                "-out", keyPath.toString()), "Key Generation");

        // Extract public key
        String publicKey = runOpenSSL(List.of(
                "openssl", "pkey",
                "-in", keyPath.toString(),
                "-pubout"), "Public Key Extraction");

        log.info("Key pair generated successfully for alias {}", sanitizedAlias);
        return publicKey;
    }

    /**
     * Signs a hash using a private key.
     */
    public String signHash(String alias, String dataHashBase64, String algorithm) throws OpenSSLException {
        String sanitizedAlias = sanitizeAlias(alias);
        validateAlgorithm(algorithm);
        byte[] dataHash = validateAndDecodeBase64(dataHashBase64, "dataHash", 64);
        Path keyPath = validatePathWithinBase(keyStoragePath, sanitizedAlias + ".pem");

        if (!Files.exists(keyPath)) {
            throw new OpenSSLException("Key not found: " + sanitizedAlias);
        }

        Path inputPath = null;
        Path outputPath = null;
        try {
            inputPath = Files.createTempFile("hash", ".bin");
            outputPath = Files.createTempFile("sig", ".bin");
            Files.write(inputPath, dataHash);

            runOpenSSLWithOutput(List.of(
                    "openssl", "pkeyutl",
                    "-sign",
                    "-inkey", keyPath.toString(),
                    "-in", inputPath.toString(),
                    "-out", outputPath.toString()), "Signing", outputPath.toFile());

            byte[] signature = Files.readAllBytes(outputPath);
            log.info("Hash signed successfully with key {}", sanitizedAlias);
            return Base64.getEncoder().encodeToString(signature);

        } catch (Exception e) {
            throw new OpenSSLException("Signing failed: " + e.getMessage(), e);
        } finally {
            deleteIfExists(inputPath);
            deleteIfExists(outputPath);
        }
    }

    /**
     * Generates a Certificate Signing Request.
     */
    public String generateCsr(String alias, String commonName, String organization, String country)
            throws OpenSSLException {
        String sanitizedAlias = sanitizeAlias(alias);
        String sanitizedCn = sanitizeDnComponent(commonName, "commonName");
        String sanitizedOrg = sanitizeDnComponent(organization, "organization");
        String sanitizedCountry = sanitizeDnComponent(country, "country");

        Path keyPath = validatePathWithinBase(keyStoragePath, sanitizedAlias + ".pem");
        String subjectDn = "/CN=" + sanitizedCn + "/O=" + sanitizedOrg + "/C=" + sanitizedCountry;

        Path csrPath = null;
        try {
            csrPath = Files.createTempFile("request", ".csr");

            runOpenSSLWithOutput(List.of(
                    "openssl", "req", "-new",
                    "-key", keyPath.toString(),
                    "-out", csrPath.toString(),
                    "-subj", subjectDn), "CSR Generation", csrPath.toFile());

            String csr = Files.readString(csrPath);
            log.info("CSR generated for {}", sanitizedCn);
            return csr;

        } catch (Exception e) {
            throw new OpenSSLException("CSR generation failed: " + e.getMessage(), e);
        } finally {
            deleteIfExists(csrPath);
        }
    }

    /**
     * Verifies a signature against a public key/certificate.
     */
    public boolean verifySignature(String certPem, String dataHashBase64, String signatureBase64)
            throws OpenSSLException {
        validatePem(certPem, "CERTIFICATE");
        byte[] dataHash = validateAndDecodeBase64(dataHashBase64, "dataHash", 64);
        byte[] signature = validateAndDecodeBase64(signatureBase64, "signature", 8192);

        Path certPath = null;
        Path pubKeyPath = null;
        Path hashPath = null;
        Path sigPath = null;

        try {
            certPath = Files.createTempFile("cert", ".pem");
            pubKeyPath = Files.createTempFile("pubkey", ".pem");
            hashPath = Files.createTempFile("hash", ".bin");
            sigPath = Files.createTempFile("sig", ".bin");

            Files.writeString(certPath, certPem);
            Files.write(hashPath, dataHash);
            Files.write(sigPath, signature);

            // Extract public key from certificate
            runOpenSSLWithOutput(List.of(
                    "openssl", "x509",
                    "-in", certPath.toString(),
                    "-pubkey", "-noout"), "Public Key Extraction", pubKeyPath.toFile());

            // Verify signature
            String output = runOpenSSL(List.of(
                    "openssl", "pkeyutl",
                    "-verify",
                    "-pubin",
                    "-inkey", pubKeyPath.toString(),
                    "-in", hashPath.toString(),
                    "-sigfile", sigPath.toString()), "Signature Verification");

            return output.contains("Signature Verified Successfully");

        } catch (Exception e) {
            log.warn("Signature verification failed: {}", e.getMessage());
            return false;
        } finally {
            deleteIfExists(certPath);
            deleteIfExists(pubKeyPath);
            deleteIfExists(hashPath);
            deleteIfExists(sigPath);
        }
    }

    /**
     * Checks if a certificate is within its validity period.
     */
    public boolean isCertificateValid(String certPem) throws OpenSSLException {
        validatePem(certPem, "CERTIFICATE");

        Path certPath = null;
        try {
            certPath = Files.createTempFile("cert", ".pem");
            Files.writeString(certPath, certPem);

            ProcessBuilder pb = new ProcessBuilder(
                    "openssl", "x509",
                    "-in", certPath.toString(),
                    "-checkend", "0");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return process.exitValue() == 0;

        } catch (Exception e) {
            throw new OpenSSLException("Certificate validity check failed: " + e.getMessage(), e);
        } finally {
            deleteIfExists(certPath);
        }
    }

    /**
     * Extracts serial number from a certificate.
     */
    public String extractSerialNumber(String certPem) throws OpenSSLException {
        validatePem(certPem, "CERTIFICATE");

        Path certPath = null;
        try {
            certPath = Files.createTempFile("cert", ".pem");
            Files.writeString(certPath, certPem);

            String output = runOpenSSL(List.of(
                    "openssl", "x509",
                    "-in", certPath.toString(),
                    "-serial", "-noout"), "Extract Serial Number");

            if (output.startsWith("serial=")) {
                return output.substring(7).trim();
            }
            return null;

        } catch (Exception e) {
            throw new OpenSSLException("Serial extraction failed: " + e.getMessage(), e);
        } finally {
            deleteIfExists(certPath);
        }
    }

    // ============= INTERNAL HELPERS =============

    private String runOpenSSL(List<String> command, String operationName) throws OpenSSLException {
        log.debug("Executing OpenSSL: {} - {}", operationName, String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new OpenSSLException(operationName + " timed out");
            }

            if (process.exitValue() != 0) {
                throw new OpenSSLException(operationName + " failed: " + output);
            }

            return output.toString().trim();

        } catch (OpenSSLException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenSSLException(operationName + " failed: " + e.getMessage(), e);
        }
    }

    private void runOpenSSLWithOutput(List<String> command, String operationName, File outputFile)
            throws OpenSSLException {
        log.debug("Executing OpenSSL with output: {} - {}", operationName, String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new OpenSSLException(operationName + " timed out");
            }

            if (process.exitValue() != 0) {
                StringBuilder error = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                }
                throw new OpenSSLException(operationName + " failed: " + error);
            }

        } catch (OpenSSLException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenSSLException(operationName + " failed: " + e.getMessage(), e);
        }
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(keyStoragePath);
            Files.createDirectories(certStoragePath);
        } catch (Exception e) {
            log.warn("Could not create storage directories: {}", e.getMessage());
        }
    }

    private void deleteIfExists(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                log.warn("Failed to delete temp file {}: {}", path, e.getMessage());
            }
        }
    }

    /**
     * Exception for OpenSSL operation failures.
     */
    public static class OpenSSLException extends Exception {
        public OpenSSLException(String message) {
            super(message);
        }

        public OpenSSLException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
