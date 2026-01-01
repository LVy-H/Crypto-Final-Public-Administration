package com.gov.crypto.caauthority.registration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KycData(
        @JsonProperty("cccdNumber") String cccdNumber,
        @JsonProperty("fullName") String fullName,
        @JsonProperty("email") String email,
        @JsonProperty("province") String province,
        @JsonProperty("district") String district,
        @JsonProperty("organization") String organization,
        @JsonProperty("country") String country) {
}
