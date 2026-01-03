package com.gov.crypto.controller;

import com.gov.crypto.repository.OfficerAssignmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for officer assignment operations.
 * Used by doc-service for auto-assignment of countersigners.
 */
@RestController
@RequestMapping("/api/v1/officers")
public class OfficerController {

    private final OfficerAssignmentRepository assignmentRepo;

    public OfficerController(OfficerAssignmentRepository assignmentRepo) {
        this.assignmentRepo = assignmentRepo;
    }

    /**
     * Get officers assigned to a specific CA/RA.
     * Used by doc-service for auto-assignment of countersigners.
     */
    @GetMapping("/by-ca/{caId}")
    public ResponseEntity<List<OfficerDto>> getOfficersByCa(@PathVariable UUID caId) {
        List<OfficerDto> officers = assignmentRepo.findByCaIdAndActiveTrue(caId)
                .stream()
                .map(a -> new OfficerDto(
                        a.getOfficer().getId(),
                        a.getOfficer().getUsername(),
                        a.getCaType().name()))
                .toList();

        return ResponseEntity.ok(officers);
    }

    /**
     * Get all CAs managed by a specific officer.
     */
    @GetMapping("/{officerId}/managed-cas")
    public ResponseEntity<List<UUID>> getManagedCas(@PathVariable UUID officerId) {
        // Would need to fetch User first, simplified for now
        return ResponseEntity.ok(List.of());
    }

    /**
     * DTO for officer information.
     */
    public record OfficerDto(UUID id, String username, String caType) {
    }
}
