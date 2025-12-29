package com.gov.crypto.caauthority.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting/decrypting CA private keys at rest.
 * 
 * Uses AES-256-GCM for authenticated encryption.
 * Master key must be provided via environment variable CA_MASTER_KEY.
 * 
 * SECURITY: In production, use a proper key management system (Vault/HSM).
 */
@Service
public class KeyEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(KeyEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENCRYPTED_PREFIX = "ENC:";

    private final SecretKey masterKey;
    private final boolean encryptionEnabled;

    public KeyEncryptionService(
            @Value("${CA_MASTER_KEY:}") String masterKeyBase64,
            @Value("${app.ca.encrypt-keys:true}") boolean encryptKeys) {

        this.encryptionEnabled = encryptKeys && masterKeyBase64 != null && !masterKeyBase64.isEmpty();

        if (this.encryptionEnabled) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
                if (keyBytes.length != 32) {
                    throw new IllegalArgumentException(
                            "CA_MASTER_KEY must be 32 bytes (256 bits), got " + keyBytes.length);
                }
                this.masterKey = new SecretKeySpec(keyBytes, "AES");
                log.info("CA key encryption enabled with AES-256-GCM");
            } catch (Exception e) {
                throw new IllegalStateException("Invalid CA_MASTER_KEY: " + e.getMessage(), e);
            }
        } else {
            this.masterKey = null;
            log.warn(
                    "CA key encryption DISABLED - private keys stored in plaintext. Set CA_MASTER_KEY for production.");
        }
    }

    /**
     * Encrypt a private key PEM string.
     */
    public String encrypt(String plaintext) throws Exception {
        if (!encryptionEnabled || masterKey == null) {
            return plaintext;
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);

        return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * Decrypt an encrypted private key.
     */
    public String decrypt(String encrypted) throws Exception {
        if (!encryptionEnabled || masterKey == null) {
            return encrypted;
        }

        // Check if already decrypted (plaintext PEM)
        if (!encrypted.startsWith(ENCRYPTED_PREFIX)) {
            log.warn("Key is not encrypted (missing prefix), returning as-is");
            return encrypted;
        }

        byte[] combined = Base64.getDecoder().decode(encrypted.substring(ENCRYPTED_PREFIX.length()));

        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /**
     * Write encrypted key to file.
     */
    public void writeEncryptedKey(Path path, String privateKeyPem) throws Exception {
        String content = encrypt(privateKeyPem);
        Files.writeString(path, content);
        log.debug("Wrote {} key to {}", encryptionEnabled ? "encrypted" : "plaintext", path);
    }

    /**
     * Read and decrypt key from file.
     */
    public String readDecryptedKey(Path path) throws Exception {
        String content = Files.readString(path);
        return decrypt(content);
    }

    /**
     * Check if encryption is enabled.
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * Generate a new master key (for initial setup).
     */
    public static String generateMasterKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
