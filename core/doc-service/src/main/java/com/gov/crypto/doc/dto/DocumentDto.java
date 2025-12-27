package com.gov.crypto.doc.dto;

import com.gov.crypto.doc.entity.DocumentClassification;
import com.gov.crypto.doc.entity.DocumentVisibility;
import java.util.UUID;
import java.time.Instant;

public record DocumentDto(
        UUID id,
        String name,
        UUID ownerId,
        UUID orgId,
        DocumentClassification classification,
        DocumentVisibility visibility,
        String signatureId,
        Instant signedAt,
        Instant createdAt) {
}
