package com.gov.crypto.validationservice.dto;

import java.util.List;

/**
 * Response for document verification with enriched certificate chain data.
 */
public record DocumentVerifyResponse(
        boolean valid,
        String signerSubject,
        String signedAt,
        String algorithm,
        String details,
        List<CertificateInfo> certificateChain,
        CertificateInfo tsaCertificate) {
    public record CertificateInfo(
            String subject,
            String issuer,
            String validFrom,
            String validTo,
            String serialNumber,
            String algorithm) {
    }

    // Convenience constructor for failure responses
    public static DocumentVerifyResponse failure(String message) {
        return new DocumentVerifyResponse(false, null, null, null, message, null, null);
    }
}
