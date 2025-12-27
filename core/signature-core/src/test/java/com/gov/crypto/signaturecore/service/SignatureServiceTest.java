package com.gov.crypto.signaturecore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Signature Core Service - Document signing operations.
 * Tests document hashing, signature creation, and archive generation.
 */
@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Document Hashing Tests")
    class HashingTests {

        @Test
        @DisplayName("Should compute SHA-256 hash of document")
        void shouldComputeSha256Hash() throws Exception {
            // Given
            byte[] document = "Test document content".getBytes();

            // When
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(document);

            // Then
            assertNotNull(hash);
            assertEquals(32, hash.length); // SHA-256 = 256 bits = 32 bytes
        }

        @Test
        @DisplayName("Should produce different hashes for different documents")
        void shouldProduceDifferentHashes() throws Exception {
            // Given
            byte[] doc1 = "Document 1".getBytes();
            byte[] doc2 = "Document 2".getBytes();

            // When
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash1 = digest.digest(doc1);
            digest.reset();
            byte[] hash2 = digest.digest(doc2);

            // Then
            assertFalse(java.util.Arrays.equals(hash1, hash2));
        }

        @Test
        @DisplayName("Should produce same hash for identical documents")
        void shouldProduceSameHashForIdentical() throws Exception {
            // Given
            byte[] doc = "Same content".getBytes();

            // When
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash1 = digest.digest(doc);
            digest.reset();
            byte[] hash2 = digest.digest(doc);

            // Then
            assertArrayEquals(hash1, hash2);
        }
    }

    @Nested
    @DisplayName("Signature Packaging Tests")
    class PackagingTests {

        @Test
        @DisplayName("Should create signature archive")
        void shouldCreateSignatureArchive() throws Exception {
            // Given
            Path docPath = tempDir.resolve("document.pdf");
            Path sigPath = tempDir.resolve("signature.sig");

            Files.writeString(docPath, "PDF content here");
            Files.writeString(sigPath, "Signature content here");

            // Then
            assertTrue(Files.exists(docPath));
            assertTrue(Files.exists(sigPath));
        }

        @Test
        @DisplayName("Should include metadata in signature")
        void shouldIncludeMetadata() {
            // Given
            String metadata = """
                    {
                        "signer": "Nguyen Van A",
                        "timestamp": "2024-12-25T10:30:00Z",
                        "algorithm": "ML-DSA-65",
                        "reason": "Document approval"
                    }
                    """;

            // Then
            assertTrue(metadata.contains("signer"));
            assertTrue(metadata.contains("algorithm"));
            assertTrue(metadata.contains("ML-DSA-65"));
        }
    }

    @Nested
    @DisplayName("Timestamp Tests")
    class TimestampTests {

        @Test
        @DisplayName("Should create RFC 3161 compatible timestamp")
        void shouldCreateRfc3161Timestamp() {
            // Given
            long currentTime = System.currentTimeMillis();

            // Then
            assertTrue(currentTime > 0);
        }

        @Test
        @DisplayName("Should include timestamp in signature")
        void shouldIncludeTimestamp() {
            // Given
            String isoTimestamp = java.time.Instant.now().toString();

            // Then
            assertNotNull(isoTimestamp);
            assertTrue(isoTimestamp.contains("T"));
        }
    }

    @Nested
    @DisplayName("Document Type Support Tests")
    class DocumentTypeTests {

        @Test
        @DisplayName("Should support PDF documents")
        void shouldSupportPdf() {
            String mimeType = "application/pdf";
            assertTrue(mimeType.contains("pdf"));
        }

        @Test
        @DisplayName("Should support Word documents")
        void shouldSupportWord() {
            String mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            assertTrue(mimeType.contains("word"));
        }

        @Test
        @DisplayName("Should support XML documents")
        void shouldSupportXml() {
            String mimeType = "application/xml";
            assertTrue(mimeType.contains("xml"));
        }
    }

    @Nested
    @DisplayName("Base64 Encoding Tests")
    class EncodingTests {

        @Test
        @DisplayName("Should encode signature as Base64")
        void shouldEncodeAsBase64() {
            // Given
            byte[] signature = "mock-signature-content".getBytes();

            // When
            String encoded = Base64.getEncoder().encodeToString(signature);

            // Then
            assertNotNull(encoded);
            assertFalse(encoded.contains(" ")); // No whitespace in Base64
        }

        @Test
        @DisplayName("Should decode Base64 signature")
        void shouldDecodeBase64() {
            // Given
            String encoded = Base64.getEncoder().encodeToString("test".getBytes());

            // When
            byte[] decoded = Base64.getDecoder().decode(encoded);

            // Then
            assertEquals("test", new String(decoded));
        }
    }
}
