package com.gov.crypto.documentservice.controller

import com.gov.crypto.documentservice.entity.Document
import com.gov.crypto.documentservice.repository.DocumentRepository
import com.gov.crypto.documentservice.service.EncryptionService
import com.gov.crypto.documentservice.service.ObjectStorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/documents")
class DocumentController(
    private val documentRepository: DocumentRepository,
    private val encryptionService: EncryptionService,
    private val objectStorageService: ObjectStorageService
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("ownerId") ownerId: String // verified by Gateway/Identity
    ): ResponseEntity<String> {
        
        // 1. Encrypt Stream (Client -> Server -> RAM -> Encryption -> MinIO)
        // For large files, use piped streams. Here using simplified memory approach.
        val encryptedData = encryptionService.encryptStream(file.inputStream)

        // 2. Upload to MinIO
        val bucket = "documents"
        val key = "${UUID.randomUUID()}-${file.originalFilename}"
        
        objectStorageService.upload(
            bucket,
            key,
            encryptedData.processedStream,
            encryptedData.payloadSize,
            "application/octet-stream"
        )
        
        // 3. Save Metadata
        val doc = Document(
            ownerId = ownerId,
            filename = file.originalFilename ?: "unknown",
            minioBucket = bucket,
            minioObjectKey = key,
            encryptedDek = Base64.getEncoder().encodeToString(encryptedData.encryptedDek)
        )
        documentRepository.save(doc)

        return ResponseEntity.ok("Document uploaded and encrypted.")
    }
}
