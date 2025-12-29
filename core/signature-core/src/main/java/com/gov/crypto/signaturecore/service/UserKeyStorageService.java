package com.gov.crypto.signaturecore.service;

import com.gov.crypto.common.pqc.PqcCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for storing and retrieving user signing keys.
 * 
 * SECURITY NOTE: In production, this should be backed by HSM via PKCS#11.
 * This implementation stores keys in memory for development/demo purposes.
 * Keys are persisted per user session and associated with userId.
 */
@Service
public class UserKeyStorageService {

    private static final Logger log = LoggerFactory.getLogger(UserKeyStorageService.class);

    private final PqcCryptoService pqcCryptoService;

    // Maps userId -> keyAlias -> stored key
    private final Map<String, Map<String, StoredKey>> userKeys = new ConcurrentHashMap<>();

    // Default key for anonymous/demo signing
    private KeyPair defaultKeyPair;

    public UserKeyStorageService(PqcCryptoService pqcCryptoService) {
        this.pqcCryptoService = pqcCryptoService;
    }

    @PostConstruct
    public void init() {
        // Generate a default key pair for demo purposes
        try {
            defaultKeyPair = pqcCryptoService.generateMlDsaKeyPair(PqcCryptoService.MlDsaLevel.ML_DSA_44);
            log.info("Default signing key pair initialized");
        } catch (Exception e) {
            log.error("Failed to initialize default key pair", e);
        }
    }

    /**
     * Generate and store a new key pair for a user.
     */
    public StoredKey generateKeyForUser(String userId, String keyAlias, PqcCryptoService.MlDsaLevel level)
            throws Exception {
        KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(level);

        StoredKey storedKey = new StoredKey(
                keyAlias,
                keyPair.getPrivate(),
                keyPair.getPublic(),
                level.name());

        userKeys.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(keyAlias, storedKey);

        log.info("Generated key '{}' for user '{}' with algorithm {}", keyAlias, userId, level.name());
        return storedKey;
    }

    /**
     * Retrieve a stored key for signing.
     * Returns the default key if no specific key is found.
     */
    public PrivateKey getPrivateKey(String userId, String keyAlias) {
        if (userId == null || userId.isEmpty()) {
            log.debug("No userId provided, using default key");
            return defaultKeyPair.getPrivate();
        }

        Map<String, StoredKey> keys = userKeys.get(userId);
        if (keys == null) {
            log.debug("No keys found for user '{}', using default key", userId);
            return defaultKeyPair.getPrivate();
        }

        StoredKey storedKey = keys.get(keyAlias);
        if (storedKey == null) {
            log.debug("Key '{}' not found for user '{}', using default key", keyAlias, userId);
            return defaultKeyPair.getPrivate();
        }

        log.debug("Retrieved key '{}' for user '{}'", keyAlias, userId);
        return storedKey.privateKey();
    }

    /**
     * Get the algorithm for a stored key.
     */
    public String getKeyAlgorithm(String userId, String keyAlias) {
        if (userId == null || userId.isEmpty()) {
            return "ML-DSA-44";
        }

        Map<String, StoredKey> keys = userKeys.get(userId);
        if (keys == null || !keys.containsKey(keyAlias)) {
            return "ML-DSA-44";
        }

        return keys.get(keyAlias).algorithm();
    }

    /**
     * Check if a user has any stored keys.
     */
    public boolean hasKeysForUser(String userId) {
        return userKeys.containsKey(userId) && !userKeys.get(userId).isEmpty();
    }

    /**
     * Internal record for stored key data.
     */
    public record StoredKey(
            String alias,
            PrivateKey privateKey,
            PublicKey publicKey,
            String algorithm) {
    }
}
