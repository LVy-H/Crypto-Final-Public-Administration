package com.gov.crypto.validationservice.service;

import com.gov.crypto.validationservice.dto.StampVerifyRequest;
import com.gov.crypto.validationservice.dto.StampVerifyResponse;
import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;

public interface ValidationService {
    VerifyResponse verifySignature(VerifyRequest request);

    /**
     * Verify a countersignature (stamp) including user signature,
     * officer signature, and optional timestamp.
     */
    StampVerifyResponse verifyStamp(StampVerifyRequest request);

    /**
     * Debug only: Generate signature for testing.
     * WARNING: Do not use in production.
     */
    String signDebug(String privateKeyPem, String dataBase64, String algorithm);

    /**
     * Debug only: Generate KeyPair and CSR for testing.
     * Returns Map with "privateKey", "publicKey", "csr".
     */
    java.util.Map<String, String> generateCsrDebug(String subjectDn, String algorithm);
}
