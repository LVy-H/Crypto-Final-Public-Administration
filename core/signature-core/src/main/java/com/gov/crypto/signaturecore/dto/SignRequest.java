package com.gov.crypto.signaturecore.dto;

/**
 * Request DTO for signing operations.
 * 
 * @param userId     Optional user ID to retrieve stored key
 * @param keyAlias   Key alias to use for signing
 * @param dataBase64 Base64-encoded data to sign
 */
public record SignRequest(String userId, String keyAlias, String dataBase64) {
}
