package com.gov.crypto.caauthority.controller;

import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.service.HierarchicalCaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/certificates")
public class AdminCertificateController {

    private final HierarchicalCaService caService;

    public AdminCertificateController(HierarchicalCaService caService) {
        this.caService = caService;
    }

    @GetMapping
    public ResponseEntity<List<IssuedCertificate>> getAllCertificates(
            @RequestParam(required = false) String status) {
        if (status != null) {
            try {
                IssuedCertificate.CertStatus certStatus = IssuedCertificate.CertStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(caService.getCertificatesByStatus(certStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(caService.getAllIssuedCertificates());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(caService.getCertificateStats());
    }
}
