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
}
