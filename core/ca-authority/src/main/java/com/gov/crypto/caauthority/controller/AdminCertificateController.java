package com.gov.crypto.caauthority.controller;

import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.service.CaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/certificates")
public class AdminCertificateController {

    private final CaService caService;

    public AdminCertificateController(CaService caService) {
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

    @GetMapping("/requests/pending")
    public ResponseEntity<List<IssuedCertificate>> getPendingRequests() {
        return ResponseEntity.ok(caService.getCertificatesByStatus(IssuedCertificate.CertStatus.PENDING));
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<IssuedCertificate> approveRequest(@PathVariable java.util.UUID id) throws Exception {
        return ResponseEntity.ok(caService.approveCertificate(id));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable java.util.UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Admin rejected";
        caService.rejectCertificateRequest(id, reason);
        return ResponseEntity.ok().build();
    }
}
