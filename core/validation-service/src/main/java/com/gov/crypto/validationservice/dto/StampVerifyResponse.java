package com.gov.crypto.validationservice.dto;

/**
 * Response DTO for stamp verification.
 */
public record StampVerifyResponse(
        boolean valid,
        boolean userSignatureValid,
        boolean officerSignatureValid,
        boolean timestampValid,
        boolean userCertValid,
        boolean officerCertValid,
        String message,
        String details) {
    /**
     * Simple constructor for quick responses.
     */
    public StampVerifyResponse(boolean valid, String message) {
        this(valid, false, false, false, false, false, message, null);
    }
}
