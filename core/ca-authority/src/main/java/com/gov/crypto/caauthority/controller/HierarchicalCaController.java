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

    // ============ CSR-Based CA Initialization (Recommended) ============

    /**
     * Generate CSR for Subordinate CA initialization.
     * 
     * This is the CORRECT workflow per Decree 23/2025:
     * 1. Call this endpoint to generate CSR
     * 2. Submit CSR to National Root CA for signing
     * 3. Receive signed certificate from National Root
     * 4. Call /ca/upload-cert to activate this CA
     * 
     * @param request Contains: name, algorithm (mldsa87, mldsa65, ecdsa384)
     * @return CSR in PEM format for submission to National Root
     */
    @PostMapping("/init-csr")
    public ResponseEntity<Map<String, Object>> generateCaCsr(@RequestBody Map<String, String> request) {
        try {
            String name = request.getOrDefault("name", "Ministry Subordinate CA");
            String algorithm = request.getOrDefault("algorithm", "mldsa87");

            var result = caService.generateCaCsr(name, algorithm);

            return ResponseEntity.ok(Map.of(
                    "pendingCaId", result.pendingCaId(),
                    "csrPem", result.csrPem(),
                    "algorithm", algorithm,
                    "instructions", "Submit this CSR to National Root CA for signing, then call POST /ca/upload-cert"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Upload signed certificate from National Root CA.
     * 
     * After receiving the signed certificate from National Root,
     * upload it here to activate this as a Subordinate CA.
     * 
     * @param request Contains: pendingCaId, certificatePem (from National Root)
     * @return Activated CA details
     */
    @PostMapping("/upload-cert")
    public ResponseEntity<Map<String, Object>> uploadSignedCertificate(@RequestBody Map<String, String> request) {
        try {
            String pendingCaId = request.get("pendingCaId");
            String certificatePem = request.get("certificatePem");
            String nationalRootCertPem = request.get("nationalRootCertPem"); // Optional: for chain validation

            if (pendingCaId == null || certificatePem == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Missing pendingCaId or certificatePem"));
            }

            CertificateAuthority activatedCa = caService.activateCaWithSignedCert(
                    pendingCaId, certificatePem, nationalRootCertPem);

            return ResponseEntity.ok(Map.of(
                    "id", activatedCa.getId(),
                    "name", activatedCa.getName(),
                    "algorithm", activatedCa.getAlgorithm(),
                    "status", activatedCa.getStatus().name(),
                    "validUntil", activatedCa.getValidUntil().toString(),
                    "message", "CA activated successfully as Subordinate CA"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ Subordinate CA Creation ============

    /**
     * Issue service certificate for mTLS (ML-DSA-65)
     */
    @PostMapping("/internal/issue")
    public ResponseEntity<Map<String, Object>> issueServiceCertificate(@RequestBody Map<String, Object> request) {
        try {
            String serviceName = (String) request.get("serviceName");
            @SuppressWarnings("unchecked")
            List<String> dnsNames = (List<String>) request.getOrDefault("dnsNames", List.of());
            int validDays = (Integer) request.getOrDefault("validDays", 365);

            var result = caService.issueServiceCertificate(serviceName, dnsNames, validDays);
            return ResponseEntity.ok(Map.of(
                    "certificate", result.certificate(),
                    "privateKey", result.privateKey(),
                    "caCertificate", result.caCertificate()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create Generic Subordinate CA
     */
    @PostMapping("/{parentId}/subordinate")
    public ResponseEntity<Map<String, Object>> createSubordinate(
            @PathVariable UUID parentId,
            @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String label = (String) request.getOrDefault("label", "Subordinate CA");
            String algo = (String) request.getOrDefault("algorithm", "mldsa65");
            int validDays = (Integer) request.getOrDefault("validDays", 365);
            String typeStr = (String) request.getOrDefault("type", "ISSUING_CA");
            CertificateAuthority.CaType type = CertificateAuthority.CaType.valueOf(typeStr);

            CertificateAuthority subCa = caService.createSubordinate(parentId, name, type, algo, label, validDays);

            return ResponseEntity.ok(Map.of(
                    "id", subCa.getId(),
                    "name", subCa.getName(),
                    "algorithm", subCa.getAlgorithm(),
                    "parentCaId", subCa.getParentCa().getId(),
                    "validUntil", subCa.getValidUntil().toString(),
                    "type", subCa.getType().name(),
                    "level", subCa.getHierarchyLevel()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create Provincial CA (ML-DSA-87) - Legacy Wrapper
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
     * Create District RA (ML-DSA-65) - Legacy Wrapper
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
     * Register Third-Party RA (External Keys)
     */
    @PostMapping("/{parentId}/external-ra")
    public ResponseEntity<Map<String, Object>> registerExternalRa(
            @PathVariable UUID parentId,
            @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String csrPem = (String) request.get("csr");
            String label = (String) request.getOrDefault("label", "External RA");
            int validDays = (Integer) request.getOrDefault("validDays", 365);

            CertificateAuthority extRa = caService.registerExternalRa(parentId, name, csrPem, label, validDays);

            return ResponseEntity.ok(Map.of(
                    "id", extRa.getId(),
                    "name", extRa.getName(),
                    "algorithm", extRa.getAlgorithm(),
                    "parentCaId", extRa.getParentCa().getId(),
                    "validUntil", extRa.getValidUntil().toString(),
                    "type", extRa.getType().name(),
                    "level", extRa.getHierarchyLevel(),
                    "certificate", extRa.getCertificate()));
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
     * Get CRL for a CA
     */
    @GetMapping(value = "/crl/{caId}", produces = "text/plain")
    public ResponseEntity<String> getCrl(@PathVariable UUID caId) {
        try {
            String crlPem = caService.generateCrl(caId);
            return ResponseEntity.ok(crlPem);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating CRL: " + e.getMessage());
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
                "level", ca.getHierarchyLevel(),
                "label", ca.getLabel(),
                "algorithm", ca.getAlgorithm(),
                "status", ca.getStatus().name())).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Get all CAs at a specific hierarchy level
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Map<String, Object>>> getCasByLevel(@PathVariable int level) {
        try {
            var cas = caService.getCasByLevel(level);
            var result = cas.stream().map(ca -> Map.<String, Object>of(
                    "id", ca.getId(),
                    "name", ca.getName(),
                    "algorithm", ca.getAlgorithm(),
                    "status", ca.getStatus().name(),
                    "validUntil", ca.getValidUntil().toString())).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Invalid level: " + e.getMessage())));
        }
    }

    /**
     * Get all CAs (flat list, for tree building)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> listAllCas() {
        var cas = caService.getAllCas();
        var result = cas.stream().map(ca -> Map.<String, Object>of(
                "id", ca.getId(),
                "name", ca.getName(),
                "algorithm", ca.getAlgorithm(),
                "type", ca.getType() != null ? ca.getType().name() : "UNKNOWN",
                "parentCaId", ca.getParentCa() != null ? ca.getParentCa().getId() : "",
                "organizationId", ca.getOrganizationId() != null ? ca.getOrganizationId() : "",
                "status", ca.getStatus().name(),
                "level", ca.getHierarchyLevel())).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Create subordinate CA for a specific organization.
     * The CA is linked to the org and can issue certs for that org's members.
     */
    @PostMapping("/org/{orgId}")
    public ResponseEntity<Map<String, Object>> createCaForOrganization(
            @PathVariable UUID orgId,
            @RequestBody Map<String, Object> request) {
        try {
            UUID parentCaId = UUID.fromString((String) request.get("parentCaId"));
            String name = (String) request.get("name");
            String label = (String) request.getOrDefault("label", "Organization CA");
            String algo = (String) request.getOrDefault("algorithm", "mldsa65");
            int validDays = request.containsKey("validDays") ? (Integer) request.get("validDays") : 1825;

            CertificateAuthority subCa = caService.createSubordinate(
                    parentCaId, name, CertificateAuthority.CaType.RA, algo, label, validDays);

            // Link CA to organization
            subCa.setOrganizationId(orgId);
            caService.saveCa(subCa);

            return ResponseEntity.ok(Map.of(
                    "id", subCa.getId(),
                    "name", subCa.getName(),
                    "organizationId", orgId,
                    "algorithm", subCa.getAlgorithm(),
                    "parentCaId", subCa.getParentCa().getId(),
                    "validUntil", subCa.getValidUntil().toString(),
                    "level", subCa.getHierarchyLevel()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get CA linked to a specific organization
     */
    @GetMapping("/org/{orgId}")
    public ResponseEntity<Map<String, Object>> getCaByOrganization(@PathVariable UUID orgId) {
        try {
            var ca = caService.getCaByOrganizationId(orgId);
            if (ca == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                    "id", ca.getId(),
                    "name", ca.getName(),
                    "organizationId", ca.getOrganizationId(),
                    "algorithm", ca.getAlgorithm(),
                    "status", ca.getStatus().name(),
                    "level", ca.getHierarchyLevel()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
