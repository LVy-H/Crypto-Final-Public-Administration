package com.gov.crypto.raservice.dto;

public record RegistrationRequest(
        String username,
        String email,
        String algorithm, // e.g., "ML-DSA-44"
        String kycData // Placeholder for ID card number, etc.
) {
}
