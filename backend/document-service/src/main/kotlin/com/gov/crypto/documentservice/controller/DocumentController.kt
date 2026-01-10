package com.gov.crypto.documentservice.controller

import com.gov.crypto.document.service.AsicService
import com.gov.crypto.document.service.CmsSigner
import com.gov.crypto.documentservice.entity.Document
import com.gov.crypto.documentservice.repository.DocumentRepository
import com.gov.crypto.documentservice.service.EncryptionService
import com.gov.crypto.documentservice.service.ObjectStorageService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/documents")
class DocumentController(
    private val documentRepository: DocumentRepository,
    private val encryptionService: EncryptionService,
    private val objectStorageService: ObjectStorageService,
    private val asicService: AsicService,
    private val cmsSigner: CmsSigner
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("ownerId", required = false) ownerId: String? // Optional for test
    ): ResponseEntity<Map<String, String>> {
        
        // MVP: Simple upload to get DocID (Ignoring encryption for pure ASiC test flow)
        // In Prod: Use EncryptionService as before.
        
        val bucket = "documents"
        val key = "${UUID.randomUUID()}-${file.originalFilename}"
        
        objectStorageService.upload(
            bucket,
            key,
            file.inputStream,
            file.size,
            file.contentType ?: "application/octet-stream"
        )
        
        val doc = Document(
            ownerId = ownerId ?: "anonymous",
            filename = file.originalFilename ?: "unknown",
            minioBucket = bucket,
            minioObjectKey = key,
            encryptedDek = "" // Skipped for MVP Asic Test
        )
        val saved = documentRepository.save(doc)

        return ResponseEntity.ok(mapOf(
            "docId" to saved.id.toString(),
            "status" to "UPLOADED"
        ))
    }

    data class FinalizeRequest(
        val docId: String,
        val signature: String, // Base64
        val certificate: String // Base64
    )

    @PostMapping("/finalize-asic")
    fun finalizeAsic(@RequestBody request: FinalizeRequest): ResponseEntity<ByteArray> {
        // Fix 1: ID is Long
        val docId = request.docId.toLong()
        val doc = documentRepository.findById(docId).orElseThrow { RuntimeException("Doc not found") }
        
        // 1. Fetch Original Document from MinIO
        // Fix 2: Read Stream to ByteArray
        val inputStream = objectStorageService.download(doc.minioBucket, doc.minioObjectKey)
        val originalBytes = inputStream.readAllBytes()
        
        // 2. Decode Client inputs
        val signatureBytes = Base64.getDecoder().decode(request.signature)
        val certBytes = Base64.getDecoder().decode(request.certificate)
        
        // 3. Create CMS structure (Wrapper)
        val cmsBytes = cmsSigner.wrapInCms(originalBytes, signatureBytes, certBytes)
        
        // 4. Create ASiC Container (Zip)
        val asicBytes = asicService.createContainer(
            originalFilename = doc.filename,
            documentBytes = originalBytes,
            cmsSignature = cmsBytes
        )
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"package.asic\"")
            .contentType(MediaType.parseMediaType("application/vnd.etsi.asic-e+zip"))
            .body(asicBytes)
    }

    @PostMapping("/countersign")
    fun countersign(
        @RequestParam("file") file: MultipartFile, 
        @RequestParam("signature") signatureBase64: String,
        @RequestParam("certificate") certificateBase64: String
    ): ResponseEntity<ByteArray> {
        // 1. Read existing ASiC container
        val currentContainerBytes = file.bytes
        
        // 2. Decode new signature inputs
        val signatureBytes = Base64.getDecoder().decode(signatureBase64)
        val certBytes = Base64.getDecoder().decode(certificateBase64)
        
        // 3. Extract original document to sign (Simplified: Assuming we sign the *original content* again, 
        // OR we sign the previous signature. For PQC ASiC, we usually sign the original doc hash again 
        // or effectively add a parallel signature.
        // For this MVP, we just wrap the original content (extracted) or just the raw bytes. 
        // Ideally we need to extract 'test.txt' from zip to wrap in CMS.
        // Let's simplified: We assume client sends signature of the content.
        // We just need to wrap it in CMS. But wait, CMS needs the content to wrap/digest? 
        // Yes, if detachable. 
        // Let's extract original file from ZIP first.
        
        // Helper to find original file (not mimetype, not META-INF)
        var originalContent: ByteArray? = null
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(currentContainerBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.name.startsWith("META-INF") && entry.name != "mimetype") {
                    originalContent = zis.readAllBytes()
                    break
                }
                entry = zis.nextEntry
            }
        }
        
        if (originalContent == null) throw RuntimeException("Original document not found in ASiC")
        
        // 4. Create new CMS
        val cmsBytes = cmsSigner.wrapInCms(originalContent!!, signatureBytes, certBytes)
        
        // 5. Add to container
        val newContainerBytes = asicService.addSignature(currentContainerBytes, cmsBytes)
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"package_countersigned.asic\"")
            .contentType(MediaType.parseMediaType("application/vnd.etsi.asic-e+zip"))
            .body(newContainerBytes)
    }

    @PostMapping("/verify-asic")
    fun verifyAsic(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Any>> {
        val result = asicService.verifyContainer(file.bytes)
        
        return ResponseEntity.ok(mapOf(
            "valid" to result.valid,
            "signatureCount" to result.signatureCount,
            "documentName" to (result.documentName ?: "unknown"),
            "documentSize" to (result.documentSize ?: 0),
            "errorMessage" to (result.errorMessage ?: ""),
            "signatures" to result.signatureResults.map { sig ->
                mapOf(
                    "path" to sig.signaturePath,
                    "valid" to sig.valid,
                    "message" to sig.message,
                    "signerName" to (sig.signerName ?: ""),
                    "certificateSubject" to (sig.certificateSubject ?: ""),
                    "certificateIssuer" to (sig.certificateIssuer ?: ""),
                    "certificateNotBefore" to (sig.certificateNotBefore ?: ""),
                    "certificateNotAfter" to (sig.certificateNotAfter ?: ""),
                    "timestamp" to (sig.timestamp ?: "")
                )
            }
        ))
    }
}
