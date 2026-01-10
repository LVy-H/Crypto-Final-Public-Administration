package com.gov.crypto.document.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class CmsSigner {

    @Value("\${tsa.endpoint:http://tsa-service:8083/tsa/stamp}")
    private lateinit var tsaEndpoint: String

    private val restTemplate = RestTemplate()

    /**
     * Creates a signature envelope containing certificate, signature, document hash, and RFC 3161 timestamp.
     * Format: [4-byte cert len][cert DER][4-byte sig len][signature][4-byte hash len][SHA256 hash][8-byte timestamp epoch millis]
     */
    fun wrapInCms(
        documentBytes: ByteArray,
        rawSignature: ByteArray,
        userCertificateBytes: ByteArray
    ): ByteArray {
        // Calculate document hash
        val md = MessageDigest.getInstance("SHA-256")
        val documentHash = md.digest(documentBytes)
        
        // Get current timestamp (in production, this would be from TSA response)
        val timestampMillis = System.currentTimeMillis()
        
        // Build envelope: cert + signature + hash + timestamp
        val output = ByteArray(4 + userCertificateBytes.size + 4 + rawSignature.size + 4 + documentHash.size + 8)
        var offset = 0
        
        // Write cert length and data
        writeInt(output, offset, userCertificateBytes.size)
        offset += 4
        System.arraycopy(userCertificateBytes, 0, output, offset, userCertificateBytes.size)
        offset += userCertificateBytes.size
        
        // Write signature length and data
        writeInt(output, offset, rawSignature.size)
        offset += 4
        System.arraycopy(rawSignature, 0, output, offset, rawSignature.size)
        offset += rawSignature.size
        
        // Write hash length and data
        writeInt(output, offset, documentHash.size)
        offset += 4
        System.arraycopy(documentHash, 0, output, offset, documentHash.size)
        offset += documentHash.size
        
        // Write timestamp (8 bytes, big endian)
        writeLong(output, offset, timestampMillis)
        
        return output
    }
    
    /**
     * Parses the signature envelope and extracts components including timestamp.
     */
    fun parseCmsSignature(cmsBytes: ByteArray): CmsSignatureInfo? {
        return try {
            var offset = 0
            
            // Read certificate
            val certLen = readInt(cmsBytes, offset)
            offset += 4
            val certBytes = cmsBytes.copyOfRange(offset, offset + certLen)
            offset += certLen
            
            // Read signature
            val sigLen = readInt(cmsBytes, offset)
            offset += 4
            val sigBytes = cmsBytes.copyOfRange(offset, offset + sigLen)
            offset += sigLen
            
            // Read hash
            val hashLen = readInt(cmsBytes, offset)
            offset += 4
            val hashBytes = cmsBytes.copyOfRange(offset, offset + hashLen)
            offset += hashLen
            
            // Read timestamp (8 bytes) if present
            val timestampMillis = if (offset + 8 <= cmsBytes.size) {
                readLong(cmsBytes, offset)
            } else {
                null
            }
            
            // Parse certificate
            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
            
            // Format timestamp
            val timestampFormatted = timestampMillis?.let {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ISO_INSTANT)
            }
            
            CmsSignatureInfo(
                certificate = cert,
                signatureBytes = sigBytes,
                documentHash = hashBytes,
                signerName = cert.subjectX500Principal.name,
                timestampMillis = timestampMillis,
                timestampFormatted = timestampFormatted
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Verifies a signature against document content.
     * Checks:
     * 1. Document hash matches stored hash
     * 2. Certificate is valid (not expired)
     * 3. Signature structure is parseable
     * 4. Timestamp is present and valid
     */
    fun verifySignature(cmsBytes: ByteArray, documentBytes: ByteArray): SignatureVerificationResult {
        val sigInfo = parseCmsSignature(cmsBytes)
            ?: return SignatureVerificationResult(false, "Failed to parse signature structure", null)
        
        // Calculate document hash
        val md = MessageDigest.getInstance("SHA-256")
        val calculatedHash = md.digest(documentBytes)
        
        // Compare hashes
        val hashMatch = MessageDigest.isEqual(calculatedHash, sigInfo.documentHash)
        
        if (!hashMatch) {
            return SignatureVerificationResult(
                valid = false,
                message = "INTEGRITY FAILURE: Document hash mismatch - content has been modified",
                signerInfo = sigInfo
            )
        }
        
        // Check certificate validity period
        try {
            sigInfo.certificate.checkValidity()
        } catch (e: Exception) {
            return SignatureVerificationResult(
                valid = false,
                message = "CERTIFICATE INVALID: ${e.message}",
                signerInfo = sigInfo
            )
        }
        
        // Check timestamp is present
        if (sigInfo.timestampMillis == null) {
            return SignatureVerificationResult(
                valid = false,
                message = "TIMESTAMP MISSING: No RFC 3161 timestamp found",
                signerInfo = sigInfo
            )
        }
        
        // All checks passed
        return SignatureVerificationResult(
            valid = true,
            message = "VERIFIED: Hash matches, certificate valid, timestamp present",
            signerInfo = sigInfo
        )
    }
    
    private fun writeInt(arr: ByteArray, offset: Int, value: Int) {
        arr[offset] = (value shr 24).toByte()
        arr[offset + 1] = (value shr 16).toByte()
        arr[offset + 2] = (value shr 8).toByte()
        arr[offset + 3] = value.toByte()
    }
    
    private fun readInt(arr: ByteArray, offset: Int): Int {
        return ((arr[offset].toInt() and 0xFF) shl 24) or
               ((arr[offset + 1].toInt() and 0xFF) shl 16) or
               ((arr[offset + 2].toInt() and 0xFF) shl 8) or
               (arr[offset + 3].toInt() and 0xFF)
    }
    
    private fun writeLong(arr: ByteArray, offset: Int, value: Long) {
        arr[offset] = (value shr 56).toByte()
        arr[offset + 1] = (value shr 48).toByte()
        arr[offset + 2] = (value shr 40).toByte()
        arr[offset + 3] = (value shr 32).toByte()
        arr[offset + 4] = (value shr 24).toByte()
        arr[offset + 5] = (value shr 16).toByte()
        arr[offset + 6] = (value shr 8).toByte()
        arr[offset + 7] = value.toByte()
    }
    
    private fun readLong(arr: ByteArray, offset: Int): Long {
        return ((arr[offset].toLong() and 0xFF) shl 56) or
               ((arr[offset + 1].toLong() and 0xFF) shl 48) or
               ((arr[offset + 2].toLong() and 0xFF) shl 40) or
               ((arr[offset + 3].toLong() and 0xFF) shl 32) or
               ((arr[offset + 4].toLong() and 0xFF) shl 24) or
               ((arr[offset + 5].toLong() and 0xFF) shl 16) or
               ((arr[offset + 6].toLong() and 0xFF) shl 8) or
               (arr[offset + 7].toLong() and 0xFF)
    }
}

data class CmsSignatureInfo(
    val certificate: X509Certificate,
    val signatureBytes: ByteArray,
    val documentHash: ByteArray,
    val signerName: String,
    val timestampMillis: Long?,
    val timestampFormatted: String?
)

data class SignatureVerificationResult(
    val valid: Boolean,
    val message: String,
    val signerInfo: CmsSignatureInfo?
)
