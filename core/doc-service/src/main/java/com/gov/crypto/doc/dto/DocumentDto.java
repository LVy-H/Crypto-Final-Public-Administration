package com.gov.crypto.doc.dto;

import com.gov.crypto.doc.entity.ApprovalStatus;
import com.gov.crypto.doc.entity.DocumentClassification;
import com.gov.crypto.doc.entity.DocumentVisibility;
import java.util.UUID;
import java.time.Instant;

/**
 * Document DTO with signature, encryption, and countersign metadata.
 */
public record DocumentDto(
        UUID id,
        String name,
        UUID ownerId,
        UUID orgId,
        DocumentClassification classification,
        DocumentVisibility visibility,
        String contentHash,
        String contentType,
        Long fileSize,
        String signatureId,
        boolean signed,
        String signatureAlgorithm,
        boolean encrypted,
        String encryptionAlgorithm,
        // Countersign fields
        UUID assignedCountersignerId,
        UUID countersignatureId,
        ApprovalStatus approvalStatus,
        Instant approvedAt,
        String rejectionReason,
        // Timestamps
        Instant signedAt,
        Instant createdAt) {
}
