package com.gov.crypto.validationservice.dto;

/**
 * Request DTO for verifying a countersignature (stamp).
 */
public record StampVerifyRequest(
        String documentHash,
        String userSignature,
        String userCertPem,
        String officerSignature,
        String officerCertPem,
        String timestampToken) {
}
