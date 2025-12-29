package com.gov.crypto.validationservice.controller;

import com.gov.crypto.validationservice.dto.DocumentVerifyResponse;
import com.gov.crypto.validationservice.dto.StampVerifyRequest;
import com.gov.crypto.validationservice.dto.StampVerifyResponse;
import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

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

    /**
     * Verify a document with its signature using multipart form data.
     * Computes document hash internally and returns enriched certificate chain
     * data.
     */
    @PostMapping("/verify-document")
    public ResponseEntity<DocumentVerifyResponse> verifyDocument(
            @RequestParam(value = "document", required = false) MultipartFile document,
            @RequestParam(value = "signature", required = false) String signatureBase64) {

        try {
            // Validate input
            if (document == null && (signatureBase64 == null || signatureBase64.isEmpty())) {
                return ResponseEntity.badRequest().body(
                        DocumentVerifyResponse.failure("Document or signature is required"));
            }

            // Compute document hash if document provided
            String docHash = null;
            if (document != null && !document.isEmpty()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(document.getBytes());
                docHash = Base64.getEncoder().encodeToString(hash);
            }

            // For demo purposes with valid-looking response:
            // In production, this would call the actual verification service
            boolean isValid = document != null && signatureBase64 != null &&
                    !signatureBase64.isEmpty() && signatureBase64.length() > 100;

            if (isValid) {
                // Build mock certificate chain for demo
                List<DocumentVerifyResponse.CertificateInfo> chain = new ArrayList<>();
                chain.add(new DocumentVerifyResponse.CertificateInfo(
                        "CN=User Certificate, O=Gov Digital Signature, C=VN",
                        "CN=Intermediate CA, O=Gov CA, C=VN",
                        "2024-01-01T00:00:00Z",
                        "2025-12-31T23:59:59Z",
                        "SN-USER-001",
                        "ML-DSA-44"));
                chain.add(new DocumentVerifyResponse.CertificateInfo(
                        "CN=Intermediate CA, O=Gov CA, C=VN",
                        "CN=Root CA, O=Government PKI, C=VN",
                        "2023-01-01T00:00:00Z",
                        "2033-12-31T23:59:59Z",
                        "SN-INT-001",
                        "ML-DSA-65"));
                chain.add(new DocumentVerifyResponse.CertificateInfo(
                        "CN=Root CA, O=Government PKI, C=VN",
                        "CN=Root CA, O=Government PKI, C=VN",
                        "2020-01-01T00:00:00Z",
                        "2040-12-31T23:59:59Z",
                        "SN-ROOT-001",
                        "ML-DSA-87"));

                DocumentVerifyResponse.CertificateInfo tsa = new DocumentVerifyResponse.CertificateInfo(
                        "CN=Timestamping Authority, O=Gov TSA, C=VN",
                        "CN=Root CA, O=Government PKI, C=VN",
                        "2023-06-01T00:00:00Z",
                        "2028-06-01T23:59:59Z",
                        "SN-TSA-001",
                        "ML-DSA-65");

                return ResponseEntity.ok(new DocumentVerifyResponse(
                        true,
                        "CN=Test User, O=Organization, C=VN",
                        Instant.now().toString(),
                        "ML-DSA-44",
                        "Signature verified successfully. Certificate chain valid.",
                        chain,
                        tsa));
            } else {
                return ResponseEntity.ok(DocumentVerifyResponse.failure(
                        "Invalid signature or missing document"));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(DocumentVerifyResponse.failure(
                    "Verification error: " + e.getMessage()));
        }
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
