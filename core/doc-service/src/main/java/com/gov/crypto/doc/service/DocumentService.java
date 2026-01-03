package com.gov.crypto.doc.service;

import com.gov.crypto.doc.client.IdentityServiceClient;
import com.gov.crypto.doc.dto.CreateDocumentRequest;
import com.gov.crypto.doc.dto.DocumentDto;
import com.gov.crypto.doc.dto.SaveSignatureRequest;
import com.gov.crypto.doc.entity.Document;
import com.gov.crypto.doc.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;
    private final FileStorageService fileStorageService;
    private final IdentityServiceClient identityClient;

    public DocumentService(DocumentRepository repository,
            FileStorageService fileStorageService,
            IdentityServiceClient identityClient) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.identityClient = identityClient;
    }

    // === Read Operations ===

    public List<DocumentDto> getPublicDocuments() {
        return repository.findPublicDocuments().stream().map(this::toDto).toList();
    }

    public List<DocumentDto> getByOwner(UUID ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(this::toDto).toList();
    }

    public List<DocumentDto> getByOrg(UUID orgId) {
        return repository.findByOrgId(orgId).stream().map(this::toDto).toList();
    }

    public List<DocumentDto> getAccessibleByUser(UUID userId) {
        return repository.findAccessibleByUser(userId).stream().map(this::toDto).toList();
    }

    public DocumentDto getById(UUID id) {
        return repository.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    public Document getEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    // === Write Operations ===

    /**
     * Create new document record.
     * Note: Content storage with encryption requires separate call with user's
     * public key.
     */
    @Transactional
    public DocumentDto create(CreateDocumentRequest request, UUID ownerId) {
        log.info("Creating document: {} for owner: {}", request.filename(), ownerId);

        Document doc = new Document();
        doc.setName(request.filename());
        doc.setOwnerId(ownerId);
        doc.setClassification(request.classification());
        doc.setVisibility(request.visibility());
        doc.setContentHash(request.contentHash());
        doc.setContentType(request.contentType());
        doc.setFileSize(request.fileSize());
        doc.setEncrypted(request.encrypt());

        Document saved = repository.save(doc);
        log.info("Document created: {}", saved.getId());

        return toDto(saved);
    }

    /**
     * Store document content with encryption.
     */
    @Transactional
    public DocumentDto storeContent(UUID docId, byte[] content, PublicKey ownerPublicKey)
            throws Exception {
        log.info("Storing encrypted content for document: {}", docId);

        Document doc = getEntityById(docId);

        FileStorageService.StoredDocument stored = fileStorageService.storeEncrypted(docId, content, ownerPublicKey);

        doc.setStoragePath(stored.storagePath());
        doc.setEncryptionIv(stored.encryptionIv());
        doc.setWrappedKey(stored.encapsulation());
        doc.setEncryptionAlgorithm(stored.encryptionAlgorithm());
        doc.setEncrypted(true);

        return toDto(repository.save(doc));
    }

    /**
     * Save signature to document after signing.
     */
    @Transactional
    public DocumentDto saveSignature(UUID docId, SaveSignatureRequest request) {
        log.info("Saving signature to document: {}", docId);

        Document doc = getEntityById(docId);

        doc.setSignatureBase64(request.signatureBase64());
        doc.setTimestampBase64(request.timestampBase64());
        doc.setSigningKeyAlias(request.keyAlias());
        doc.setSignatureAlgorithm(request.algorithm());
        doc.setCertificateSerial(request.certificateSerial());
        doc.setSignedAt(Instant.now());

        Document saved = repository.save(doc);
        log.info("Signature saved for document: {}", docId);

        return toDto(saved);
    }

    /**
     * Download document content (decryption requires user's private key on client
     * side).
     * Returns encrypted content - client must decrypt.
     */
    public byte[] downloadContent(UUID docId) throws Exception {
        Document doc = getEntityById(docId);

        if (doc.getStoragePath() == null) {
            throw new IllegalStateException("Document has no stored content: " + docId);
        }

        if (doc.isEncrypted()) {
            return fileStorageService.loadPlain(docId); // Returns encrypted bytes
        } else {
            return fileStorageService.loadPlain(docId);
        }
    }

    // === Approval Workflow ===

    /**
     * Submit document for officer approval.
     * Auto-assigns countersigner based on user's RA.
     */
    @Transactional
    public DocumentDto submitForApproval(UUID docId, UUID userId, UUID userAssignedCaId) {
        log.info("Submitting document {} for approval by user {}", docId, userId);

        Document doc = getEntityById(docId);

        // Verify ownership
        if (!doc.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Only document owner can submit for approval");
        }

        // Verify document is signed
        if (doc.getSignatureBase64() == null) {
            throw new IllegalStateException("Document must be signed before submitting for approval");
        }

        // Verify not already submitted
        if (doc.getApprovalStatus() != com.gov.crypto.doc.entity.ApprovalStatus.DRAFT) {
            throw new IllegalStateException("Document already submitted for approval");
        }

        // Auto-assign countersigner (for now, use a simple assignment)
        // TODO: Implement OfficerAssignmentService for round-robin
        UUID assignedOfficer = findOfficerForCa(userAssignedCaId);
        doc.setAssignedCountersignerId(assignedOfficer);
        doc.setApprovalStatus(com.gov.crypto.doc.entity.ApprovalStatus.PENDING_COUNTERSIGN);

        Document saved = repository.save(doc);
        log.info("Document {} submitted for approval, assigned to officer {}", docId, assignedOfficer);

        return toDto(saved);
    }

    /**
     * Get pending documents for officer.
     */
    public List<DocumentDto> getPendingForOfficer(UUID officerId) {
        return repository.findByAssignedCountersignerIdAndApprovalStatus(
                officerId, com.gov.crypto.doc.entity.ApprovalStatus.PENDING_COUNTERSIGN)
                .stream().map(this::toDto).toList();
    }

    /**
     * Save countersign result from officer.
     */
    @Transactional
    public DocumentDto saveCountersign(UUID docId, UUID officerId,
            com.gov.crypto.doc.dto.CountersignRequest request) {
        log.info("Saving countersign for document {} by officer {}", docId, officerId);

        Document doc = getEntityById(docId);

        // ABAC: Only assigned officer can countersign
        if (!officerId.equals(doc.getAssignedCountersignerId())) {
            throw new IllegalArgumentException("Only assigned officer can countersign this document");
        }

        if (request.approved()) {
            doc.setCountersignatureId(request.countersignatureId());
            doc.setApprovalStatus(com.gov.crypto.doc.entity.ApprovalStatus.APPROVED);
            doc.setApprovedAt(java.time.Instant.now());
            log.info("Document {} approved by officer {}", docId, officerId);
        } else {
            doc.setApprovalStatus(com.gov.crypto.doc.entity.ApprovalStatus.REJECTED);
            doc.setRejectionReason(request.rejectionReason());
            log.info("Document {} rejected by officer {}: {}", docId, officerId, request.rejectionReason());
        }

        return toDto(repository.save(doc));
    }

    /**
     * Make document public (after approval).
     */
    @Transactional
    public DocumentDto makePublic(UUID docId, UUID userId) {
        log.info("Making document {} public", docId);

        Document doc = getEntityById(docId);

        // ABAC: Only owner can make public
        if (!doc.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Only document owner can change visibility");
        }

        // Must be approved
        if (doc.getApprovalStatus() != com.gov.crypto.doc.entity.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Document must be approved before making public");
        }

        doc.setVisibility(com.gov.crypto.doc.entity.DocumentVisibility.PUBLIC);
        return toDto(repository.save(doc));
    }

    /**
     * Find an officer for the given CA.
     * Queries identity-service for officers assigned to this CA,
     * then selects one using round-robin.
     */
    private UUID findOfficerForCa(UUID caId) {
        if (caId == null) {
            log.warn("No CA ID provided, cannot auto-assign officer");
            return null;
        }

        List<IdentityServiceClient.OfficerInfo> officers = identityClient.getOfficersForCa(caId);

        if (officers.isEmpty()) {
            log.warn("No officers found for CA: {}", caId);
            return null;
        }

        // Round-robin: pick random officer for load balancing
        int index = ThreadLocalRandom.current().nextInt(officers.size());
        UUID selected = officers.get(index).id();

        log.info("Auto-assigned officer {} for CA {}", selected, caId);
        return selected;
    }

    // === DTO Mapping ===

    public DocumentDto toDto(Document doc) {
        return new DocumentDto(
                doc.getId(),
                doc.getName(),
                doc.getOwnerId(),
                doc.getOrgId(),
                doc.getClassification(),
                doc.getVisibility(),
                doc.getContentHash(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getSignatureId(),
                doc.getSignatureBase64() != null, // isSigned
                doc.getSignatureAlgorithm(),
                doc.isEncrypted(),
                doc.getEncryptionAlgorithm(),
                // Countersign fields
                doc.getAssignedCountersignerId(),
                doc.getCountersignatureId(),
                doc.getApprovalStatus(),
                doc.getApprovedAt(),
                doc.getRejectionReason(),
                // Timestamps
                doc.getSignedAt(),
                doc.getCreatedAt());
    }
}
