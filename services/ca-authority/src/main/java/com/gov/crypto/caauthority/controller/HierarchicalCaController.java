package com.gov.crypto.caauthority.controller;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.service.HierarchicalCaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ca")
public class HierarchicalCaController {

    private final HierarchicalCaService caService;

    public HierarchicalCaController(HierarchicalCaService caService) {
        this.caService = caService;
    }

    /**
     * Initialize Root CA (ML-DSA-87)
     */
    @PostMapping("/root/init")
    public ResponseEntity<Map<String, Object>> initializeRootCa(@RequestBody Map<String, String> request) {
        try {
            String name = request.getOrDefault("name", "National Root CA");
            CertificateAuthority rootCa = caService.initializeRootCa(name);
            return ResponseEntity.ok(Map.of(
                    "id", rootCa.getId(),
                    "name", rootCa.getName(),
                    "algorithm", rootCa.getAlgorithm(),
                    "validUntil", rootCa.getValidUntil().toString(),
                    "status", rootCa.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create Internal Services CA signed by Root CA (ML-DSA-65)
     * Automatically generates mTLS certificates for all microservices.
     */
    @PostMapping("/internal/init")
    public ResponseEntity<Map<String, Object>> createInternalServicesCa(@RequestBody Map<String, String> request) {
        try {
            UUID rootCaId = UUID.fromString(request.get("rootCaId"));
            CertificateAuthority internalCa = caService.createInternalServicesCa(rootCaId);
            return ResponseEntity.ok(Map.of(
                    "id", internalCa.getId(),
                    "name", internalCa.getName(),
                    "algorithm", internalCa.getAlgorithm(),
                    "parentCaId", internalCa.getParentCa().getId(),
                    "validUntil", internalCa.getValidUntil().toString(),
                    "status", internalCa.getStatus().name(),
                    "message", "mTLS certificates generated for all services"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create Provincial CA (ML-DSA-87)
     */
    @PostMapping("/provincial")
    public ResponseEntity<Map<String, Object>> createProvincialCa(@RequestBody Map<String, String> request) {
        try {
            UUID parentId = UUID.fromString(request.get("parentCaId"));
            String name = request.get("name");
            CertificateAuthority provincialCa = caService.createProvincialCa(parentId, name);
            return ResponseEntity.ok(Map.of(
                    "id", provincialCa.getId(),
                    "name", provincialCa.getName(),
                    "algorithm", provincialCa.getAlgorithm(),
                    "parentCaId", provincialCa.getParentCa().getId(),
                    "validUntil", provincialCa.getValidUntil().toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create District RA (ML-DSA-65)
     */
    @PostMapping("/district")
    public ResponseEntity<Map<String, Object>> createDistrictRa(@RequestBody Map<String, String> request) {
        try {
            UUID parentId = UUID.fromString(request.get("parentCaId"));
            String name = request.get("name");
            CertificateAuthority districtRa = caService.createDistrictRa(parentId, name);
            return ResponseEntity.ok(Map.of(
                    "id", districtRa.getId(),
                    "name", districtRa.getName(),
                    "algorithm", districtRa.getAlgorithm(),
                    "parentCaId", districtRa.getParentCa().getId(),
                    "validUntil", districtRa.getValidUntil().toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Issue user certificate
     */
    @PostMapping("/issue")
    public ResponseEntity<Map<String, Object>> issueCertificate(@RequestBody Map<String, String> request) {
        try {
            UUID issuingRaId = UUID.fromString(request.get("issuingRaId"));
            String csr = request.get("csr");
            String subjectDn = request.get("subjectDn");

            IssuedCertificate cert = caService.issueUserCertificate(issuingRaId, csr, subjectDn);
            return ResponseEntity.ok(Map.of(
                    "id", cert.getId(),
                    "serialNumber", cert.getSerialNumber(),
                    "certificate", cert.getCertificate(),
                    "validUntil", cert.getValidUntil().toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get certificate chain
     */
    @GetMapping("/chain/{caId}")
    public ResponseEntity<List<String>> getCertificateChain(@PathVariable UUID caId) {
        List<String> chain = caService.getCertificateChain(caId);
        return ResponseEntity.ok(chain);
    }

    /**
     * Revoke certificate
     */
    @PostMapping("/revoke/{certId}")
    public ResponseEntity<Map<String, String>> revokeCertificate(
            @PathVariable UUID certId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.getOrDefault("reason", "Unspecified");
            caService.revokeCertificate(certId, reason);
            return ResponseEntity.ok(Map.of("status", "revoked", "certId", certId.toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Revoke a CA/RA and all its subordinates (cascade)
     */
    @PostMapping("/revoke-ca/{caId}")
    public ResponseEntity<Map<String, String>> revokeCa(
            @PathVariable UUID caId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.getOrDefault("reason", "Unspecified");
            caService.revokeCa(caId, reason);
            return ResponseEntity.ok(Map.of("status", "revoked", "caId", caId.toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all subordinate CAs under a given CA
     */
    @GetMapping("/subordinates/{caId}")
    public ResponseEntity<List<Map<String, Object>>> getSubordinates(@PathVariable UUID caId) {
        var subordinates = caService.getAllSubordinates(caId);
        var result = subordinates.stream().map(ca -> Map.<String, Object>of(
                "id", ca.getId(),
                "name", ca.getName(),
                "level", ca.getLevel().name(),
                "algorithm", ca.getAlgorithm(),
                "status", ca.getStatus().name())).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Get all CAs at a specific level (ROOT, PROVINCIAL, DISTRICT)
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Map<String, Object>>> getCasByLevel(@PathVariable String level) {
        try {
            var caLevel = CertificateAuthority.CaLevel.valueOf(level.toUpperCase());
            var cas = caService.getCasByLevel(caLevel);
            var result = cas.stream().map(ca -> Map.<String, Object>of(
                    "id", ca.getId(),
                    "name", ca.getName(),
                    "algorithm", ca.getAlgorithm(),
                    "status", ca.getStatus().name(),
                    "validUntil", ca.getValidUntil().toString())).toList();
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Invalid level. Use: ROOT, PROVINCIAL, or DISTRICT")));
        }
    }
}
