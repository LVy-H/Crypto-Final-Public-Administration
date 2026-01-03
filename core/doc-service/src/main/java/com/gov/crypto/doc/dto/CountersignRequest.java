package com.gov.crypto.doc.dto;

import java.util.UUID;

/**
 * Request DTO for saving countersign result to document.
 */
public record CountersignRequest(
        UUID countersignatureId, // From stamp service
        boolean approved,
        String rejectionReason // Required if approved=false
) {
}
