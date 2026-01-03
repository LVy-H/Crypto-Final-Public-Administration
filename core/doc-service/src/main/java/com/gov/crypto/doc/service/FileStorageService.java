package com.gov.crypto.doc.service;

import com.gov.crypto.common.encryption.MlKemEncryptionService;
import com.gov.crypto.common.encryption.MlKemEncryptionService.EncryptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

/**
 * File Storage Service with ML-KEM encryption at rest.
 * 
 * Documents are encrypted before storage using:
 * - ML-KEM-768 for key encapsulation (FIPS 203)
 * - AES-256-GCM for symmetric encryption
 * 
 * Each document has a unique encryption key encapsulated
 * with the owner's public key - only they can decrypt.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${document.storage.path:/app/data/documents}")
    private String storagePath;

    private final MlKemEncryptionService encryptionService;

    public FileStorageService(MlKemEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Store document with encryption.
     * 
     * @param docId          Document ID (used as filename)
     * @param content        Raw document content
     * @param ownerPublicKey Owner's ML-KEM public key for encryption
     * @return StoredDocument with storage path and encryption metadata
     */
    public StoredDocument storeEncrypted(UUID docId, byte[] content, PublicKey ownerPublicKey)
            throws IOException, GeneralSecurityException {

        log.info("Storing encrypted document: {} ({} bytes)", docId, content.length);

        // Ensure storage directory exists
        Path storageDir = Paths.get(storagePath);
        Files.createDirectories(storageDir);

        // Encrypt content with ML-KEM + AES-256-GCM
        EncryptionResult encrypted = encryptionService.encrypt(content, ownerPublicKey);

        // Store encrypted content
        Path contentPath = storageDir.resolve(docId + ".enc");
        Files.write(contentPath, encrypted.ciphertext());

        log.info("Document {} encrypted and stored at {}", docId, contentPath);

        return new StoredDocument(
                contentPath.toString(),
                encrypted.ivBase64(),
                encrypted.encapsulationBase64(),
                "ML-KEM-768+AES-256-GCM");
    }

    /**
     * Store document without encryption (for public documents).
     */
    public String storePlain(UUID docId, byte[] content) throws IOException {
        log.info("Storing plain document: {} ({} bytes)", docId, content.length);

        Path storageDir = Paths.get(storagePath);
        Files.createDirectories(storageDir);

        Path contentPath = storageDir.resolve(docId + ".bin");
        Files.write(contentPath, content);

        return contentPath.toString();
    }

    /**
     * Load and decrypt document.
     * 
     * @param docId           Document ID
     * @param encryptionIv    Base64-encoded IV
     * @param encapsulation   Base64-encoded ML-KEM encapsulation
     * @param ownerPrivateKey Owner's ML-KEM private key
     * @return Decrypted document content
     */
    public byte[] loadAndDecrypt(UUID docId, String encryptionIv, String encapsulation,
            PrivateKey ownerPrivateKey)
            throws IOException, GeneralSecurityException {

        log.info("Loading and decrypting document: {}", docId);

        Path contentPath = Paths.get(storagePath, docId + ".enc");
        byte[] ciphertext = Files.readAllBytes(contentPath);

        EncryptionResult encrypted = EncryptionResult.fromBase64(
                java.util.Base64.getEncoder().encodeToString(ciphertext),
                encryptionIv,
                encapsulation);

        return encryptionService.decrypt(encrypted, ownerPrivateKey);
    }

    /**
     * Load plain (unencrypted) document.
     */
    public byte[] loadPlain(UUID docId) throws IOException {
        Path contentPath = Paths.get(storagePath, docId + ".bin");
        return Files.readAllBytes(contentPath);
    }

    /**
     * Delete document file.
     */
    public void delete(UUID docId, boolean encrypted) throws IOException {
        String extension = encrypted ? ".enc" : ".bin";
        Path contentPath = Paths.get(storagePath, docId + extension);
        Files.deleteIfExists(contentPath);
        log.info("Deleted document file: {}", contentPath);
    }

    /**
     * Check if document file exists.
     */
    public boolean exists(UUID docId, boolean encrypted) {
        String extension = encrypted ? ".enc" : ".bin";
        Path contentPath = Paths.get(storagePath, docId + extension);
        return Files.exists(contentPath);
    }

    /**
     * Stored document result.
     */
    public record StoredDocument(
            String storagePath,
            String encryptionIv,
            String encapsulation,
            String encryptionAlgorithm) {
    }
}
