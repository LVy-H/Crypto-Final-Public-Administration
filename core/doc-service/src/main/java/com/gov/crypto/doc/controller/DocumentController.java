package com.gov.crypto.doc.controller;

import com.gov.crypto.doc.dto.CountersignRequest;
import com.gov.crypto.doc.dto.CreateDocumentRequest;
import com.gov.crypto.doc.dto.DocumentDto;
import com.gov.crypto.doc.dto.SaveSignatureRequest;
import com.gov.crypto.doc.entity.Document;
import com.gov.crypto.doc.entity.DocumentVisibility;
import com.gov.crypto.doc.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Document Controller with CRUD operations, signature storage, and ABAC
 * enforcement.
 * 
 * Endpoints:
 * - GET /api/v1/doc/public - Get public documents
 * - GET /api/v1/doc/my - Get user's documents
 * - GET /api/v1/doc/{id} - Get document by ID (ABAC enforced)
 * - POST /api/v1/doc - Create new document
 * - POST /api/v1/doc/{id}/signature - Save signature to document
 * - GET /api/v1/doc/{id}/download - Download document content (ABAC enforced)
 * - POST /api/v1/doc/{id}/submit-approval - Submit for officer approval
 * - GET /api/v1/doc/pending-approval - Officer's pending queue
 * - POST /api/v1/doc/{id}/countersign - Save countersign result
 * - POST /api/v1/doc/{id}/make-public - Make document public
 */
@RestController
@RequestMapping("/api/v1/doc")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    // === Read Endpoints ===

    @GetMapping("/public")
    public List<DocumentDto> getPublicDocuments() {
        return service.getPublicDocuments();
    }

    @GetMapping("/my")
    public List<DocumentDto> getMyDocuments(@RequestHeader("X-User-Id") UUID userId) {
        return service.getByOwner(userId);
    }

    @GetMapping("/org/{orgId}")
    public List<DocumentDto> getOrgDocuments(@PathVariable UUID orgId) {
        return service.getByOrg(orgId);
    }

    /**
     * Get document by ID with ABAC enforcement.
     * PRIVATE documents: only owner or assigned countersigner can access.
     */
    @GetMapping("/{id}")
    public DocumentDto getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {

        Document doc = service.getEntityById(id);

        // ABAC: Check visibility
        if (doc.getVisibility() == DocumentVisibility.PRIVATE) {
            boolean isOwner = doc.getOwnerId().equals(userId);
            boolean isAssignedOfficer = userId.equals(doc.getAssignedCountersignerId());

            if (!isOwner && !isAssignedOfficer) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: private document");
            }
        }

        return service.toDto(doc);
    }

    // === Write Endpoints ===

    /**
     * Create new document record.
     */
    @PostMapping
    public DocumentDto create(
            @RequestBody CreateDocumentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Creating document: {} for user: {}", request.filename(), userId);
        return service.create(request, userId);
    }

    /**
     * Save signature to document after signing.
     */
    @PostMapping("/{id}/signature")
    public DocumentDto saveSignature(
            @PathVariable UUID id,
            @RequestBody SaveSignatureRequest request) {
        log.info("Saving signature to document: {}", id);
        return service.saveSignature(id, request);
    }

    /**
     * Download document content with ABAC enforcement.
     * For encrypted documents, content is returned encrypted.
     * Client must use their ML-KEM private key to decrypt.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) throws Exception {
        log.info("Downloading document: {}", id);

        Document doc = service.getEntityById(id);

        // ABAC: Check visibility for download
        if (doc.getVisibility() == DocumentVisibility.PRIVATE) {
            boolean isOwner = doc.getOwnerId().equals(userId);
            boolean isAssignedOfficer = userId.equals(doc.getAssignedCountersignerId());

            if (!isOwner && !isAssignedOfficer) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: private document");
            }
        }

        byte[] content = service.downloadContent(id);
        String contentType = doc.getContentType() != null
                ? doc.getContentType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition",
                        "attachment; filename=\"" + doc.getName() + "\"")
                .header("X-Encrypted", String.valueOf(doc.isEncrypted()))
                .body(content);
    }

    // === Approval Workflow Endpoints ===

    /**
     * Submit document for officer approval.
     * Auto-assigns countersigner based on user's RA.
     */
    @PostMapping("/{id}/submit-approval")
    public DocumentDto submitForApproval(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Assigned-Ca", required = false) UUID userAssignedCa) {
        log.info("Submitting document {} for approval", id);
        return service.submitForApproval(id, userId, userAssignedCa);
    }

    /**
     * Get pending documents for officer.
     */
    @GetMapping("/pending-approval")
    public List<DocumentDto> getPendingApproval(
            @RequestHeader("X-User-Id") UUID officerId) {
        log.info("Getting pending documents for officer: {}", officerId);
        return service.getPendingForOfficer(officerId);
    }

    /**
     * Officer approves/rejects document.
     */
    @PostMapping("/{id}/countersign")
    public DocumentDto countersign(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID officerId,
            @RequestBody CountersignRequest request) {
        log.info("Countersigning document {} by officer {}", id, officerId);
        return service.saveCountersign(id, officerId, request);
    }

    /**
     * Owner makes document public (after approval).
     */
    @PostMapping("/{id}/make-public")
    public DocumentDto makePublic(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Making document {} public", id);
        return service.makePublic(id, userId);
    }
}
