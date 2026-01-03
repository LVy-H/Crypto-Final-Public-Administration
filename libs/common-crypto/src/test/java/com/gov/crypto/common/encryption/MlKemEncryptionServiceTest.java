package com.gov.crypto.common.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.security.Security;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security-focused tests for ML-KEM (Kyber768) encryption service.
 * Tests cryptographic correctness, key isolation, and tamper detection.
 */
class MlKemEncryptionServiceTest {

    private static MlKemEncryptionService encryptionService;

    @BeforeAll
    static void setupProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        encryptionService = new MlKemEncryptionService();
    }

    @Nested
    @DisplayName("Key Generation Tests")
    class KeyGenerationTests {

        @Test
        @DisplayName("Should generate unique key pairs")
        void shouldGenerateUniqueKeyPairs() throws Exception {
            KeyPair keyPair1 = encryptionService.generateKeyPair();
            KeyPair keyPair2 = encryptionService.generateKeyPair();

            assertNotNull(keyPair1);
            assertNotNull(keyPair2);

            // Keys should be different
            assertFalse(Arrays.equals(
                    keyPair1.getPrivate().getEncoded(),
                    keyPair2.getPrivate().getEncoded()),
                    "Each key pair should be unique");
        }

        @Test
        @DisplayName("Should generate ML-KEM-768 algorithm keys")
        void shouldGenerateMlKem768Keys() throws Exception {
            KeyPair keyPair = encryptionService.generateKeyPair();

            // BC 1.83 uses "ML-KEM" for the algorithm name
            assertTrue(keyPair.getPublic().getAlgorithm().contains("KEM") ||
                    keyPair.getPublic().getAlgorithm().contains("Kyber"),
                    "Public key should be ML-KEM/Kyber algorithm");
            assertTrue(keyPair.getPrivate().getAlgorithm().contains("KEM") ||
                    keyPair.getPrivate().getAlgorithm().contains("Kyber"),
                    "Private key should be ML-KEM/Kyber algorithm");
        }
    }

    @Nested
    @DisplayName("Encryption/Decryption Tests")
    class EncryptionDecryptionTests {

        @Test
        @DisplayName("Should encrypt and decrypt correctly with same key")
        void shouldEncryptAndDecryptWithSameKey() throws Exception {
            KeyPair keyPair = encryptionService.generateKeyPair();
            byte[] plaintext = "Sensitive government document content - Confidential".getBytes();

            MlKemEncryptionService.EncryptionResult encrypted = encryptionService.encrypt(plaintext,
                    keyPair.getPublic());
            byte[] decrypted = encryptionService.decrypt(encrypted, keyPair.getPrivate());

            assertArrayEquals(plaintext, decrypted,
                    "Decrypted content should match original plaintext");
        }

        @Test
        @DisplayName("Should produce different ciphertext for same plaintext (unique IVs)")
        void shouldProduceDifferentCiphertextForSamePlaintext() throws Exception {
            KeyPair keyPair = encryptionService.generateKeyPair();
            byte[] plaintext = "Same content encrypted twice".getBytes();

            MlKemEncryptionService.EncryptionResult encrypted1 = encryptionService.encrypt(plaintext,
                    keyPair.getPublic());
            MlKemEncryptionService.EncryptionResult encrypted2 = encryptionService.encrypt(plaintext,
                    keyPair.getPublic());

            // IVs should be different (security requirement)
            assertFalse(Arrays.equals(encrypted1.iv(), encrypted2.iv()),
                    "Each encryption should use a unique IV");

            // Ciphertexts should be different
            assertFalse(Arrays.equals(encrypted1.ciphertext(), encrypted2.ciphertext()),
                    "Ciphertext should differ due to unique IV");
        }

        @Test
        @DisplayName("Should fail decryption with wrong private key")
        void shouldFailDecryptionWithWrongKey() throws Exception {
            KeyPair correctKeyPair = encryptionService.generateKeyPair();
            KeyPair wrongKeyPair = encryptionService.generateKeyPair();
            byte[] plaintext = "Secret document".getBytes();

            MlKemEncryptionService.EncryptionResult encrypted = encryptionService.encrypt(plaintext,
                    correctKeyPair.getPublic());

            // Decrypting with wrong key should fail or produce garbage
            assertThrows(Exception.class, () -> encryptionService.decrypt(encrypted, wrongKeyPair.getPrivate()),
                    "Decryption with wrong key should fail (key isolation)");
        }
    }

    @Nested
    @DisplayName("Tamper Detection Tests")
    class TamperDetectionTests {

        @Test
        @DisplayName("Should detect tampered ciphertext")
        void shouldDetectTamperedCiphertext() throws Exception {
            KeyPair keyPair = encryptionService.generateKeyPair();
            byte[] plaintext = "Critical data".getBytes();

            MlKemEncryptionService.EncryptionResult encrypted = encryptionService.encrypt(plaintext,
                    keyPair.getPublic());

            // Tamper with ciphertext
            byte[] tamperedCiphertext = encrypted.ciphertext().clone();
            tamperedCiphertext[0] ^= 0xFF; // Flip bits

            MlKemEncryptionService.EncryptionResult tamperedResult = new MlKemEncryptionService.EncryptionResult(
                    tamperedCiphertext, encrypted.iv(), encrypted.encapsulation());

            // GCM authentication should fail
            assertThrows(Exception.class, () -> encryptionService.decrypt(tamperedResult, keyPair.getPrivate()),
                    "Tampered ciphertext should be detected by GCM authentication");
        }

        @Test
        @DisplayName("Should detect tampered IV")
        void shouldDetectTamperedIv() throws Exception {
            KeyPair keyPair = encryptionService.generateKeyPair();
            byte[] plaintext = "Integrity test".getBytes();

            MlKemEncryptionService.EncryptionResult encrypted = encryptionService.encrypt(plaintext,
                    keyPair.getPublic());

            // Tamper with IV
            byte[] tamperedIv = encrypted.iv().clone();
            tamperedIv[0] ^= 0xFF;

            MlKemEncryptionService.EncryptionResult tamperedResult = new MlKemEncryptionService.EncryptionResult(
                    encrypted.ciphertext(), tamperedIv, encrypted.encapsulation());

            // Decryption should fail or produce garbage (GCM auth tag mismatch)
            assertThrows(Exception.class, () -> encryptionService.decrypt(tamperedResult, keyPair.getPrivate()),
                    "Tampered IV should cause decryption failure");
        }
    }

    @Nested
    @DisplayName("Large Data Tests")
    class LargeDataTests {

        @Test
        @DisplayName("Should handle large document encryption")
        void shouldHandleLargeDocuments() throws Exception {
            KeyPair keyPair = encryptionService.generateKeyPair();

            // 1 MB document
            byte[] largeDocument = new byte[1024 * 1024];
            Arrays.fill(largeDocument, (byte) 'X');

            MlKemEncryptionService.EncryptionResult encrypted = encryptionService.encrypt(largeDocument,
                    keyPair.getPublic());
            byte[] decrypted = encryptionService.decrypt(encrypted, keyPair.getPrivate());

            assertArrayEquals(largeDocument, decrypted,
                    "Large documents should encrypt/decrypt correctly");
        }
    }

    @Nested
    @DisplayName("Base64 Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should serialize and deserialize encryption result")
        void shouldSerializeAndDeserialize() throws Exception {
            KeyPair keyPair = encryptionService.generateKeyPair();
            byte[] plaintext = "Serialization test".getBytes();

            MlKemEncryptionService.EncryptionResult encrypted = encryptionService.encrypt(plaintext,
                    keyPair.getPublic());

            // Serialize to Base64
            String ctB64 = encrypted.ciphertextBase64();
            String ivB64 = encrypted.ivBase64();
            String ekB64 = encrypted.encapsulationBase64();

            // Deserialize
            MlKemEncryptionService.EncryptionResult restored = MlKemEncryptionService.EncryptionResult.fromBase64(ctB64,
                    ivB64, ekB64);

            // Decrypt restored
            byte[] decrypted = encryptionService.decrypt(restored, keyPair.getPrivate());

            assertArrayEquals(plaintext, decrypted,
                    "Serialized/deserialized encryption should work correctly");
        }
    }
}
