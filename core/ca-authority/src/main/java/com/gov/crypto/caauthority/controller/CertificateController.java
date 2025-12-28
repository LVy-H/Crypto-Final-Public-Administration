package com.gov.crypto.caauthority.controller;

import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.service.HierarchicalCaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final HierarchicalCaService caService;
    private final ObjectMapper objectMapper;

    public CertificateController(HierarchicalCaService caService) {
        this.caService = caService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Request a new certificate (User facing)
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestCertificate(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String username = extractUsername(authHeader);
            String algorithm = request.getOrDefault("algorithm", "ML-DSA-44");

            IssuedCertificate certRequest = caService.createCertificateRequest(username, algorithm);

            return ResponseEntity.ok(Map.of(
                    "id", certRequest.getId(),
                    "serialNumber", certRequest.getSerialNumber(),
                    "status", certRequest.getStatus().name(),
                    "message", "Certificate request submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get my certificates
     */
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyCertificates(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = extractUsername(authHeader);
            List<IssuedCertificate> certs = caService.getUserCertificates(username);

            var result = certs.stream().map(cert -> Map.<String, Object>of(
                    "id", cert.getId(),
                    "serialNumber", cert.getSerialNumber(),
                    "subject", cert.getSubjectDn(),
                    "algorithm", cert.getIssuingCa().getAlgorithm(),
                    "notBefore", cert.getValidFrom().toString(),
                    "notAfter", cert.getValidUntil().toString(),
                    "status", cert.getStatus().name(),
                    "revoked", cert.getStatus() == IssuedCertificate.CertStatus.REVOKED)).toList();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractUsername(String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                    JsonNode node = objectMapper.readTree(payload);
                    if (node.has("sub")) {
                        return node.get("sub").asText();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "anonymous";
    }
}
