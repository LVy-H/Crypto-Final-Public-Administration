package com.gov.crypto.service;

import com.gov.crypto.cloudsign.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Secure OpenSSL Service for Cloud Sign operations.
 * 
 * All inputs are validated and sanitized before use in OpenSSL commands.
 * This service prevents command injection and path traversal attacks.
 */
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service("fileKeyStorage")
@ConditionalOnProperty(name = "hsm.enabled", havingValue = "false", matchIfMissing = true)
public class OpenSSLService implements KeyStorageService {

    private static final Logger log = LoggerFactory.getLogger(OpenSSLService.class);
    private static final int PROCESS_TIMEOUT_SECONDS = 30;

    @Value("${keystorage.path:/secure/keys}")
    private String keyStoragePath;

    /**
     * Signs a data hash using a specific private key alias and algorithm.
     * Supports PQC algorithms accessible via OpenSSL 3.6+ (e.g., ML-DSA-65).
     * 
     * @param keyAlias       Private key alias (sanitized)
     * @param dataHashBase64 Base64-encoded hash to sign (validated)
     * @param algorithm      Signing algorithm (validated against whitelist)
     * @return Base64-encoded signature
     */
    public String signHash(String keyAlias, String dataHashBase64, String algorithm) throws OpenSSLException {
        // SECURITY: Validate all inputs
        String sanitizedAlias = SecurityUtils.sanitizeKeyAlias(keyAlias);
        byte[] dataHash = SecurityUtils.validateBase64Bytes(dataHashBase64, "dataHash", 64);
        SecurityUtils.validateAlgorithm(algorithm);
        Path keyPath = SecurityUtils.validatePathWithinBase(keyStoragePath, sanitizedAlias + ".pem");

        log.info("Signing hash with key alias: {}", sanitizedAlias);

        Path inputPath = null;
        Path signaturePath = null;

        try {
            // Create secure temp files
            inputPath = Files.createTempFile("hash", ".bin");
            signaturePath = Files.createTempFile("sig", ".bin");
            Files.write(inputPath, dataHash);

            // Execute OpenSSL sign command
            runOpenSSL(List.of(
                    "openssl", "pkeyutl",
                    "-sign",
                    "-inkey", keyPath.toString(),
                    "-in", inputPath.toString(),
                    "-out", signaturePath.toString()), "Signing");

            // Read and encode signature
            byte[] signatureBytes = Files.readAllBytes(signaturePath);
            String signature = Base64.getEncoder().encodeToString(signatureBytes);
            log.info("Successfully signed hash with key {}", sanitizedAlias);
            return signature;

        } catch (Exception e) {
            log.error("Signing failed for alias {}: {}", sanitizedAlias, e.getMessage());
            throw new OpenSSLException("Signing failed: " + e.getMessage(), e);
        } finally {
            deleteIfExists(inputPath);
            deleteIfExists(signaturePath);
        }
    }

    /**
     * Generates a PQC key pair and returns the public key in PEM format.
     * 
     * @param alias     Key alias for storage (sanitized for path safety)
     * @param algorithm PQC algorithm (e.g., mldsa65)
     * @return Public key in PEM format
     */
    public String generateKeyPair(String alias, String algorithm) throws OpenSSLException {
        // SECURITY: Validate inputs
        String sanitizedAlias = SecurityUtils.sanitizeKeyAlias(alias);
        String validatedAlgorithm = SecurityUtils.validateAlgorithm(algorithm);

        ensureKeyStorage();
        Path keyPath = Path.of(keyStoragePath, sanitizedAlias + ".pem");

        log.info("Generating {} key pair with alias: {}", validatedAlgorithm, sanitizedAlias);

        try {
            // Generate private key
            runOpenSSL(List.of(
                    "openssl", "genpkey",
                    "-algorithm", validatedAlgorithm,
                    "-out", keyPath.toString()), "Key Generation");

            // Extract public key
            String publicKey = runOpenSSLWithOutput(List.of(
                    "openssl", "pkey",
                    "-in", keyPath.toString(),
                    "-pubout"), "Public Key Extraction");

            log.info("Successfully generated key pair for alias: {}", sanitizedAlias);
            return publicKey;

        } catch (Exception e) {
            log.error("Key generation failed for alias {}: {}", sanitizedAlias, e.getMessage());
            throw new OpenSSLException("Key generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a Certificate Signing Request (CSR).
     * 
     * @param alias   Key alias to use for signing the CSR
     * @param subject Subject DN (e.g., "/CN=User/O=Org/C=VN")
     * @return CSR in PEM format
     */
    public String generateCsr(String alias, String subject) throws OpenSSLException {
        // SECURITY: Validate inputs
        String sanitizedAlias = SecurityUtils.sanitizeKeyAlias(alias);
        String sanitizedSubject = SecurityUtils.sanitizeSubjectDn(subject);
        Path keyPath = SecurityUtils.validatePathWithinBase(keyStoragePath, sanitizedAlias + ".pem");

        log.info("Generating CSR for alias: {} with subject: {}", sanitizedAlias, sanitizedSubject);

        Path csrPath = null;
        try {
            csrPath = Files.createTempFile("request", ".csr");

            runOpenSSL(List.of(
                    "openssl", "req", "-new",
                    "-key", keyPath.toString(),
                    "-out", csrPath.toString(),
                    "-subj", sanitizedSubject), "CSR Generation");

            String csr = Files.readString(csrPath);
            log.info("Successfully generated CSR for alias: {}", sanitizedAlias);
            return csr;

        } catch (Exception e) {
            log.error("CSR generation failed for alias {}: {}", sanitizedAlias, e.getMessage());
            throw new OpenSSLException("CSR generation failed: " + e.getMessage(), e);
        } finally {
            deleteIfExists(csrPath);
        }
    }

    // ==================== INTERNAL HELPERS ====================

    private void runOpenSSL(List<String> command, String operationName) throws OpenSSLException {
        log.debug("Executing OpenSSL: {}", command.get(0) + " " + command.get(1));

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
                throw new OpenSSLException(operationName + " timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
            }

            if (process.exitValue() != 0) {
                throw new OpenSSLException(operationName + " failed: " + output);
            }

        } catch (OpenSSLException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenSSLException(operationName + " failed: " + e.getMessage(), e);
        }
    }

    private String runOpenSSLWithOutput(List<String> command, String operationName) throws OpenSSLException {
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

            return output.toString();

        } catch (OpenSSLException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenSSLException(operationName + " failed: " + e.getMessage(), e);
        }
    }

    private void ensureKeyStorage() {
        File dir = new File(keyStoragePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("Created key storage directory: {}", keyStoragePath);
            }
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
     * Custom exception for OpenSSL operation failures.
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
