package com.gov.crypto.caauthority.registration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CaRegistrationRequest(
        @JsonProperty("username") String username,
        @JsonProperty("email") String email,
        @JsonProperty("algorithm") String algorithm, // Enterprise: ML-DSA-65 (NIST Level 3) or ML-DSA-87 (NIST Level 5)
        @JsonProperty("kycData") KycData kycData // Structured KYC data
) {
}
