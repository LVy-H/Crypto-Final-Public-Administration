package com.gov.crypto.validationservice.controller;

import com.gov.crypto.validationservice.dto.StampVerifyRequest;
import com.gov.crypto.validationservice.dto.StampVerifyResponse;
import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verifySignature(@RequestBody VerifyRequest request) {
        return ResponseEntity.ok(validationService.verifySignature(request));
    }

    @PostMapping("/verify-stamp")
    public ResponseEntity<StampVerifyResponse> verifyStamp(@RequestBody StampVerifyRequest request) {
        return ResponseEntity.ok(validationService.verifyStamp(request));
    }

    @PostMapping("/debug/sign")
    public ResponseEntity<java.util.Map<String, String>> debugSign(@RequestBody java.util.Map<String, String> request) {
        String privateKeyPem = request.get("privateKeyPem");
        String dataBase64 = request.get("dataBase64");
        String algorithm = request.get("algorithm");

        String signature = validationService.signDebug(privateKeyPem, dataBase64, algorithm);
        return ResponseEntity.ok(java.util.Map.of("signature", signature));
    }

    @PostMapping("/debug/generate-csr")
    public ResponseEntity<java.util.Map<String, String>> debugGenerateCsr(
            @RequestBody java.util.Map<String, String> request) {
        String subjectDn = request.getOrDefault("subjectDn", "CN=Test User, C=VN");
        String algorithm = request.getOrDefault("algorithm", "ML-DSA-44");

        return ResponseEntity.ok(validationService.generateCsrDebug(subjectDn, algorithm));
    }
}
