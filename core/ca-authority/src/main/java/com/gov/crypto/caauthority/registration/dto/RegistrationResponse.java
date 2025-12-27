package com.gov.crypto.caauthority.registration.dto;

public record RegistrationResponse(
        String username,
        String status,
        String certificatePem) {
}
