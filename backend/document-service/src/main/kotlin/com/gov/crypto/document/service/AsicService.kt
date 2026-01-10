package com.gov.crypto.document.service

import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Service
class AsicService(private val cmsSigner: CmsSigner) {

    companion object {
        const val MIMETYPE = "application/vnd.etsi.asic-e+zip"
        const val MIMETYPE_FILENAME = "mimetype"
        const val SIG_FILENAME = "META-INF/signature.p7s"
    }

    fun createContainer(originalFilename: String, documentBytes: ByteArray, cmsSignature: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        try {
            val mimeEntry = ZipEntry(MIMETYPE_FILENAME).apply {
                method = ZipEntry.STORED
                size = MIMETYPE.length.toLong()
                compressedSize = MIMETYPE.length.toLong()
                crc = calculateCrc(MIMETYPE.toByteArray())
            }
            zos.putNextEntry(mimeEntry)
            zos.write(MIMETYPE.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(originalFilename))
            zos.write(documentBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(SIG_FILENAME))
            zos.write(cmsSignature)
            zos.closeEntry()

        } finally {
            zos.close()
        }

        return baos.toByteArray()
    }

    fun addSignature(containerBytes: ByteArray, newSignature: ByteArray): ByteArray {
        val newSignFilename = "META-INF/signature-" + System.currentTimeMillis() + ".p7s"
        
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(containerBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readAllBytes()
                }
                entry = zis.nextEntry
            }
        }
        
        entries[newSignFilename] = newSignature
        
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        
        try {
            entries[MIMETYPE_FILENAME]?.let { mimeBytes ->
                 val mimeEntry = ZipEntry(MIMETYPE_FILENAME).apply {
                    method = ZipEntry.STORED
                    size = mimeBytes.size.toLong()
                    compressedSize = mimeBytes.size.toLong()
                    crc = calculateCrc(mimeBytes)
                }
                zos.putNextEntry(mimeEntry)
                zos.write(mimeBytes)
                zos.closeEntry()
            }
            
            entries.forEach { (name, bytes) ->
                if (name != MIMETYPE_FILENAME) {
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        } finally {
            zos.close()
        }
        
        return baos.toByteArray()
    }

    /**
     * Fully verifies an ASiC container:
     * 1. Validates mimetype is first entry
     * 2. Extracts document content
     * 3. Parses and verifies each CMS signature
     * 4. Validates certificate chain and hash integrity
     */
    fun verifyContainer(containerBytes: ByteArray): AsicVerificationResult {
        val entries = mutableMapOf<String, ByteArray>()
        var firstEntryName: String? = null
        
        ZipInputStream(ByteArrayInputStream(containerBytes)).use { zis ->
            var entry = zis.nextEntry
            firstEntryName = entry?.name
            
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readAllBytes()
                }
                entry = zis.nextEntry
            }
        }
        
        // Validate mimetype is first
        if (firstEntryName != MIMETYPE_FILENAME) {
            return AsicVerificationResult(
                valid = false,
                errorMessage = "Invalid ASiC: mimetype must be first entry, found: $firstEntryName",
                signatureResults = emptyList()
            )
        }
        
        // Find document (not mimetype, not META-INF)
        val documentEntry = entries.entries.find { 
            !it.key.startsWith("META-INF") && it.key != MIMETYPE_FILENAME 
        }
        
        if (documentEntry == null) {
            return AsicVerificationResult(
                valid = false,
                errorMessage = "No document found in ASiC container",
                signatureResults = emptyList()
            )
        }
        
        val documentBytes = documentEntry.value
        val documentName = documentEntry.key
        
        // Find and verify all signatures
        val signatureEntries = entries.filter { 
            it.key.startsWith("META-INF/") && it.key.endsWith(".p7s") 
        }
        
        if (signatureEntries.isEmpty()) {
            return AsicVerificationResult(
                valid = false,
                errorMessage = "No signatures found in ASiC container",
                signatureResults = emptyList()
            )
        }
        
        val results = mutableListOf<SignatureCheckResult>()
        var allValid = true
        
        for ((sigPath, sigBytes) in signatureEntries) {
            val verifyResult = cmsSigner.verifySignature(sigBytes, documentBytes)
            
            val checkResult = SignatureCheckResult(
                signaturePath = sigPath,
                valid = verifyResult.valid,
                message = verifyResult.message,
                signerName = verifyResult.signerInfo?.signerName,
                certificateSubject = verifyResult.signerInfo?.certificate?.subjectX500Principal?.name,
                certificateIssuer = verifyResult.signerInfo?.certificate?.issuerX500Principal?.name,
                certificateNotBefore = verifyResult.signerInfo?.certificate?.notBefore?.toString(),
                certificateNotAfter = verifyResult.signerInfo?.certificate?.notAfter?.toString(),
                timestamp = verifyResult.signerInfo?.timestampFormatted
            )
            
            results.add(checkResult)
            
            if (!verifyResult.valid) {
                allValid = false
            }
        }
        
        return AsicVerificationResult(
            valid = allValid,
            errorMessage = if (allValid) null else "One or more signatures failed verification",
            signatureResults = results,
            documentName = documentName,
            documentSize = documentBytes.size,
            signatureCount = results.size
        )
    }

    private fun calculateCrc(data: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(data)
        return crc.value
    }
}

data class SignatureCheckResult(
    val signaturePath: String,
    val valid: Boolean,
    val message: String,
    val signerName: String?,
    val certificateSubject: String?,
    val certificateIssuer: String?,
    val certificateNotBefore: String?,
    val certificateNotAfter: String?,
    val timestamp: String?
)

data class AsicVerificationResult(
    val valid: Boolean,
    val errorMessage: String?,
    val signatureResults: List<SignatureCheckResult>,
    val documentName: String? = null,
    val documentSize: Int? = null,
    val signatureCount: Int = 0
)
