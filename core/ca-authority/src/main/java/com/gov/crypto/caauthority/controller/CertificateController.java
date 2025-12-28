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
