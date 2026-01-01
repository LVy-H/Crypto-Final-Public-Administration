package com.gov.crypto.caauthority.controller;

import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.service.HierarchicalCaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final HierarchicalCaService caService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${service.cloud-sign.url:http://cloud-sign:8084}")
    private String cloudSignUrl;

    public CertificateController(HierarchicalCaService caService,
            org.springframework.web.client.RestTemplate restTemplate) {
        this.caService = caService;
        this.restTemplate = restTemplate;
    }

    /**
     * Request a new certificate (User facing)
     * Uses Spring Security session-based authentication via Redis
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestCertificate(
            @RequestBody Map<String, String> request) {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String algorithm = request.getOrDefault("algorithm", "ML-DSA-44");
            String csrPem = request.get("csrPem");

            IssuedCertificate certRequest = caService.createCertificateRequest(username, algorithm, csrPem);

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
     * Approve a certificate request (Admin/Debug only)
     * Requires TOTP verification
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveCertificate(
            @PathVariable java.util.UUID id,
            @RequestBody Map<String, String> request) {
        try {
            String otpCode = request.get("otpCode");
            String username = getCurrentUsername();

            if (username == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            // 1. Verify TOTP
            if (!verifyTotp(username, otpCode)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing TOTP code"));
            }

            // 2. Approve
            IssuedCertificate cert = caService.approveCertificate(id);
            return ResponseEntity.ok(Map.of(
                    "id", cert.getId(),
                    "status", cert.getStatus().name(),
                    "message", "Certificate approved and issued"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean verifyTotp(String username, String code) {
        if (code == null || code.isBlank())
            return false;
        try {
            String verifyUrl = cloudSignUrl + "/api/v1/credentials/totp/verify";
            record VerifyRequest(String username, String code) {
            }

            restTemplate.postForEntity(verifyUrl, new VerifyRequest(username, code), Void.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Download certificate PEM (User facing)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadCertificate(@PathVariable java.util.UUID id) {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return ResponseEntity.status(401).build();
            }

            // Allow download if authenticated (ownership check can be stricter)
            IssuedCertificate cert = caService.getUserCertificates(username).stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Certificate not found or access denied"));

            if (cert.getCertificate() == null) {
                return ResponseEntity.status(500).body(null);
            }
            byte[] certBytes = cert.getCertificate().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(
                    certBytes);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"cert_" + id + ".crt\"")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/x-x509-ca-cert")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Get my certificates
     * Uses Spring Security session-based authentication via Redis
     */
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyCertificates() {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return ResponseEntity.status(401).build();
            }

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

    /**
     * Get username from Spring Security context (session-based auth via Redis)
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }
}
