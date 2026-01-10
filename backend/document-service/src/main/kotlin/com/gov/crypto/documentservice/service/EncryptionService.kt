package com.gov.crypto.documentservice.service

import com.gov.crypto.common.service.PqcCryptoService
import com.gov.crypto.common.util.KeyConverter
import org.bouncycastle.pqc.crypto.util.PublicKeyFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class EncryptionService(private val pqcCryptoService: PqcCryptoService) {

    private val random = SecureRandom()

    // Mock System-Wide Master Public Key (ML-KEM)
    // In real app, this comes from KMS/HSM
    // We generate one on fly for prototype context, or need to injecting it
    // For this code to compile, I'll generate a placeholder pair.
    private val systemKeyPair = pqcCryptoService.generateKeyPair(com.gov.crypto.common.service.PqcAlgorithm.ML_KEM_768)

    fun encryptStream(input: InputStream): EncryptedData {
        // 1. Generate Ephemeral AES Key (DEK)
        val dek = ByteArray(32) // AES-256
        random.nextBytes(dek)
        val secretKey = SecretKeySpec(dek, "AES")

        // 2. Encapsulate DEK with Master Public Key (ML-KEM)
        // Convert BC key pair to parameter
        val secretWithEncap = pqcCryptoService.encapsulate(systemKeyPair.public)
        // In real KEM, we use the shared secret (secretWithEncap.secret) as the seed or key for AES
        // Typically shared secret is hashed to derive AES key
        // For simplicity: We trust that extractSecret gives us enough entropy for KDF or direct use.
        // Actually, ML-KEM shared secret is 32 bytes. We can use it directly as AES key?
        // Wait, standard Hybrid encryption:
        // Client generates random r, Encapsulates r -> (Ciphertext, SharedSecret)
        // We use SharedSecret as the AES Key.
        
        val aesKeyBytes = secretWithEncap.secret
        val encryptedDek = secretWithEncap.encapsulation // This is stored in DB
        
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // 3. Encrypt Content (AES-GCM)
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec)
        
        // We need to prepend IV to the stream or store it. Prepending is standard.
        // Note: cipher input stream is tricky for GCM because authentication tag is at end.
        // Better to encrypt small files in memory or use proper streaming crypto libs.
        // For prototype, let's load to memory (assuming small docs).
        val inputBytes = input.readAllBytes()
        val cipherText = cipher.doFinal(inputBytes)
        
        val finalPayload = iv + cipherText
        
        return EncryptedData(
            processedStream = ByteArrayInputStream(finalPayload),
            payloadSize = finalPayload.size.toLong(),
            encryptedDek = encryptedDek
        )
    }

    // omitted decrypt for brevity/prototype
    
    data class EncryptedData(
        val processedStream: InputStream,
        val payloadSize: Long,
        val encryptedDek: ByteArray
    )
}
