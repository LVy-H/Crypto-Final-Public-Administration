package com.gov.crypto.doc.dto;

/**
 * Request DTO for saving signature to a document after signing.
 */
public record SaveSignatureRequest(
        String signatureBase64,
        String timestampBase64,
        String keyAlias,
        String algorithm,
        String certificateSerial) {
}
