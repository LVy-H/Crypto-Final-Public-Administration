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

    public CertificateController(HierarchicalCaService caService) {
        this.caService = caService;
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
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveCertificate(@PathVariable java.util.UUID id) {
        try {
            IssuedCertificate cert = caService.approveCertificate(id);
            return ResponseEntity.ok(Map.of(
                    "id", cert.getId(),
                    "status", cert.getStatus().name(),
                    "message", "Certificate approved and issued"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Download certificate PEM (User facing)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> downloadCertificate(@PathVariable java.util.UUID id) {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return ResponseEntity.status(401).build();
            }

            // In production, verify ownership!
            // For now, allow download if authenticated
            IssuedCertificate cert = caService.getUserCertificates(username).stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Certificate not found or acess denied"));

            return ResponseEntity.ok(Map.of("certificate", cert.getCertificate()));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
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
