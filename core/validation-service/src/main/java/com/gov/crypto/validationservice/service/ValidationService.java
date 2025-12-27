package com.gov.crypto.validationservice.service;

import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;

public interface ValidationService {
    VerifyResponse verifySignature(VerifyRequest request);
}
