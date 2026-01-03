package com.gov.crypto.doc.dto;

import com.gov.crypto.doc.entity.DocumentClassification;
import com.gov.crypto.doc.entity.DocumentVisibility;

/**
 * Request DTO for creating a new document.
 */
public record CreateDocumentRequest(
        String filename,
        String contentHash,
        String contentType,
        Long fileSize,
        byte[] content,
        DocumentClassification classification,
        DocumentVisibility visibility,
        boolean encrypt // true = encrypt with owner's ML-KEM key
) {
}
