package com.gov.crypto.caauthority.controller;

import com.gov.crypto.caauthority.model.Countersignature;
import com.gov.crypto.caauthority.service.CountersignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for countersignature (stamp) operations.
 */
@RestController
@RequestMapping("/api/v1/stamp")
public class CountersignatureController {

    private static final Logger log = LoggerFactory.getLogger(CountersignatureController.class);

    private final CountersignatureService stampService;

    public CountersignatureController(CountersignatureService stampService) {
        this.stampService = stampService;
    }

    /**
     * Apply a countersignature (stamp) to a user-signed document.
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyStamp(@RequestBody StampRequest request) {
        try {
            // TODO: Get officer ID from session/authentication context
            // For now, using a placeholder that should be replaced with actual auth
            UUID officerId = request.officerId();

            if (officerId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Officer ID is required"));
            }

            Countersignature stamp = stampService.applyStamp(
                    request.documentHash(),
                    request.userSignature(),
                    request.userCertPem(),
                    officerId,
                    request.officerCaId(),
                    request.purpose() != null ? request.purpose() : Countersignature.StampPurpose.OFFICIAL_VALIDATION);

            return ResponseEntity.ok(new StampResponse(
                    stamp.getId(),
                    stamp.getOfficerSignature(),
                    stamp.getOfficerCertPem(),
                    stamp.getTimestampToken(),
                    stamp.getStampedAt().toString(),
                    stamp.getPurpose().name()));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to apply stamp", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Stamp application failed: " + e.getMessage()));
        }
    }

    /**
     * Verify a countersignature.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyStamp(@RequestBody StampVerifyRequest request) {
        try {
            var result = stampService.verifyStamp(
                    request.documentHash(),
                    request.userSignature(),
                    request.officerSignature(),
                    request.officerCertPem());

            return ResponseEntity.ok(new StampVerifyResponse(
                    result.valid(),
                    result.message(),
                    result.details()));

        } catch (Exception e) {
            log.error("Stamp verification error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Verification failed: " + e.getMessage()));
        }
    }

    /**
     * Get a stamp by ID.
     */
    @GetMapping("/{stampId}")
    public ResponseEntity<?> getStamp(@PathVariable UUID stampId) {
        return stampService.getStamp(stampId)
                .map(stamp -> ResponseEntity.ok(new StampDetailsResponse(
                        stamp.getId(),
                        stamp.getDocumentHash(),
                        stamp.getUserSignature(),
                        stamp.getUserCertPem(),
                        stamp.getOfficerSignature(),
                        stamp.getOfficerCertPem(),
                        stamp.getOfficerId(),
                        stamp.getTimestampToken(),
                        stamp.getStampedAt().toString(),
                        stamp.getPurpose().name(),
                        stamp.getStatus().name())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Revoke a stamp.
     */
    @PostMapping("/{stampId}/revoke")
    public ResponseEntity<?> revokeStamp(@PathVariable UUID stampId, @RequestBody RevokeRequest request) {
        try {
            stampService.revokeStamp(stampId, request.revokedBy());
            return ResponseEntity.ok(Map.of(
                    "message", "Stamp revoked successfully",
                    "stampId", stampId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to revoke stamp", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Revocation failed: " + e.getMessage()));
        }
    }

    // Request/Response records

    record StampRequest(
            String documentHash,
            String userSignature,
            String userCertPem,
            UUID officerId,
            UUID officerCaId,
            Countersignature.StampPurpose purpose) {
    }

    record StampResponse(
            UUID stampId,
            String stampSignature,
            String officerCertPem,
            String timestampToken,
            String stampedAt,
            String purpose) {
    }

    record StampVerifyRequest(
            String documentHash,
            String userSignature,
            String officerSignature,
            String officerCertPem) {
    }

    record StampVerifyResponse(
            boolean valid,
            String message,
            String details) {
    }

    record StampDetailsResponse(
            UUID id,
            String documentHash,
            String userSignature,
            String userCertPem,
            String officerSignature,
            String officerCertPem,
            UUID officerId,
            String timestampToken,
            String stampedAt,
            String purpose,
            String status) {
    }

    record RevokeRequest(UUID revokedBy) {
    }
}
