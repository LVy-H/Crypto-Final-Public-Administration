package com.gov.crypto.raservice.dto;

public record RegistrationResponse(
        String username,
        String status,
        String certificatePem) {
}
