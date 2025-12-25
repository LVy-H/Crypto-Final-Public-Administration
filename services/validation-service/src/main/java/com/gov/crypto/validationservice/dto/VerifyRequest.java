package com.gov.crypto.validationservice.dto;

public record VerifyRequest(String originalDocHash, String signatureBase64, String certPem) {
}
