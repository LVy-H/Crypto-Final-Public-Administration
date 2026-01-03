package com.gov.crypto.common.encryption;

import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

/**
 * ML-KEM Encryption Service using FIPS 203 (Kyber/ML-KEM) + AES-256-GCM.
 * 
 * Provides post-quantum encryption at rest for documents.
 * Uses Bouncy Castle 1.83 KEM API with Kyber768 parameter set.
 * 
 * Security level: NIST Level 3 (equivalent to AES-192).
 * 
 * Architecture:
 * Document → AES-256-GCM(DEK) → Encrypted Document
 * DEK → Kyber768(User Key) → Encapsulation
 */
@Service
public class MlKemEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(MlKemEncryptionService.class);

    // BC 1.83 uses official FIPS 203 ML-KEM names
    private static final String KEM_ALGORITHM = "ML-KEM";
    private static final String KEY_ALGORITHM = "ML-KEM-768"; // NIST Level 3
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_LENGTH = 256;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generate Kyber768 key pair for encryption.
     * 
     * @return KeyPair with Kyber public and private keys
     */
    public KeyPair generateKeyPair() throws GeneralSecurityException {
        log.info("Generating Kyber768 key pair");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM, "BC");
        // Kyber768 doesn't need additional params, just secure random
        return kpg.generateKeyPair();
    }

    /**
     * Encrypt document using Kyber768 + AES-256-GCM.
     * 
     * Uses BC 1.83 KEM API:
     * 1. Generate encapsulated secret via KEMGenerateSpec
     * 2. Use shared secret as AES-256 key
     * 3. Encrypt document with AES-256-GCM
     * 
     * @param plaintext          Document content to encrypt
     * @param recipientPublicKey Recipient's Kyber public key
     * @return EncryptionResult containing ciphertext, IV, and encapsulation
     */
    public EncryptionResult encrypt(byte[] plaintext, PublicKey recipientPublicKey)
            throws GeneralSecurityException {
        log.debug("Encrypting {} bytes with Kyber768 + AES-256-GCM", plaintext.length);

        // Step 1: Key Encapsulation using BC KEM API
        KeyGenerator kemGen = KeyGenerator.getInstance(KEM_ALGORITHM, "BC");
        kemGen.init(new KEMGenerateSpec(recipientPublicKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation encapsulated = (SecretKeyWithEncapsulation) kemGen.generateKey();

        // Shared secret (256-bit AES key)
        byte[] sharedSecret = encapsulated.getEncoded();
        SecretKey aesKey = new SecretKeySpec(sharedSecret, 0, 32, "AES");
        byte[] encapsulation = encapsulated.getEncapsulation();

        // Step 2: Encrypt document with AES-256-GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        log.info("Document encrypted: {} → {} bytes", plaintext.length, ciphertext.length);
        return new EncryptionResult(ciphertext, iv, encapsulation);
    }

    /**
     * Decrypt document using Kyber private key.
     * 
     * @param encrypted  Encryption result from encrypt()
     * @param privateKey Recipient's Kyber private key
     * @return Decrypted document content
     */
    public byte[] decrypt(EncryptionResult encrypted, PrivateKey privateKey)
            throws GeneralSecurityException {
        log.debug("Decrypting {} bytes", encrypted.ciphertext().length);

        // Step 1: Key Decapsulation using BC KEM API
        KeyGenerator kemGen = KeyGenerator.getInstance(KEM_ALGORITHM, "BC");
        kemGen.init(new KEMExtractSpec(privateKey, encrypted.encapsulation(), "AES"));
        SecretKeyWithEncapsulation decapsulated = (SecretKeyWithEncapsulation) kemGen.generateKey();

        byte[] sharedSecret = decapsulated.getEncoded();
        SecretKey aesKey = new SecretKeySpec(sharedSecret, 0, 32, "AES");

        // Step 2: Decrypt with AES-GCM
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC");
        cipher.init(Cipher.DECRYPT_MODE, aesKey,
                new GCMParameterSpec(GCM_TAG_LENGTH, encrypted.iv()));
        byte[] plaintext = cipher.doFinal(encrypted.ciphertext());

        log.info("Document decrypted: {} bytes", plaintext.length);
        return plaintext;
    }

    /**
     * Encryption result container.
     * 
     * @param ciphertext    AES-GCM encrypted document
     * @param iv            GCM initialization vector (12 bytes)
     * @param encapsulation Kyber encapsulated key (for recipient)
     */
    public record EncryptionResult(
            byte[] ciphertext,
            byte[] iv,
            byte[] encapsulation) {
        /**
         * Serialize to Base64 JSON for storage.
         */
        public String toBase64Json() {
            return String.format(
                    "{\"ct\":\"%s\",\"iv\":\"%s\",\"ek\":\"%s\"}",
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(encapsulation));
        }

        /**
         * Get Base64-encoded ciphertext.
         */
        public String ciphertextBase64() {
            return Base64.getEncoder().encodeToString(ciphertext);
        }

        /**
         * Get Base64-encoded IV.
         */
        public String ivBase64() {
            return Base64.getEncoder().encodeToString(iv);
        }

        /**
         * Get Base64-encoded encapsulation.
         */
        public String encapsulationBase64() {
            return Base64.getEncoder().encodeToString(encapsulation);
        }

        /**
         * Parse from Base64 values.
         */
        public static EncryptionResult fromBase64(String ciphertextB64, String ivB64, String encapsulationB64) {
            return new EncryptionResult(
                    Base64.getDecoder().decode(ciphertextB64),
                    Base64.getDecoder().decode(ivB64),
                    Base64.getDecoder().decode(encapsulationB64));
        }
    }
}
