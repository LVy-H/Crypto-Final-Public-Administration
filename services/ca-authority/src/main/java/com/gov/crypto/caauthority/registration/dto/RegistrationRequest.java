package com.gov.crypto.caauthority.registration.dto;

public record RegistrationRequest(
                String username,
                String email,
                String algorithm, // Enterprise: ML-DSA-65 (NIST Level 3) or ML-DSA-87 (NIST Level 5)
                String kycData // Placeholder for ID card number, etc.
) {
}
