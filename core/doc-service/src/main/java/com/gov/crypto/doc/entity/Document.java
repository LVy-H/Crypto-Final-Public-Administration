package com.gov.crypto.doc.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "org_id")
    private UUID orgId; // null = personal document

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentVisibility visibility;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "signature_id")
    private String signatureId;

    @Column(name = "signed_at")
    private Instant signedAt;

    // === Signature Fields ===
    @Column(name = "signature_base64", columnDefinition = "TEXT")
    private String signatureBase64;

    @Column(name = "timestamp_base64", columnDefinition = "TEXT")
    private String timestampBase64;

    @Column(name = "signing_key_alias")
    private String signingKeyAlias;

    @Column(name = "signature_algorithm")
    private String signatureAlgorithm;

    @Column(name = "certificate_serial")
    private String certificateSerial;

    // === File Metadata ===
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    // === Encryption Fields (ML-KEM + AES-256-GCM) ===
    @Column(name = "encryption_iv")
    private String encryptionIv;

    @Column(name = "wrapped_key", columnDefinition = "TEXT")
    private String wrappedKey;

    @Column(name = "encryption_algorithm")
    private String encryptionAlgorithm;

    @Column(name = "is_encrypted")
    private boolean encrypted;

    // === Countersign Tracking ===
    @Column(name = "assigned_countersigner_id")
    private UUID assignedCountersignerId;

    @Column(name = "countersignature_id")
    private UUID countersignatureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public DocumentClassification getClassification() {
        return classification;
    }

    public void setClassification(DocumentClassification classification) {
        this.classification = classification;
    }

    public DocumentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DocumentVisibility visibility) {
        this.visibility = visibility;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(String signatureId) {
        this.signatureId = signatureId;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // === New Signature Field Getters/Setters ===

    public String getSignatureBase64() {
        return signatureBase64;
    }

    public void setSignatureBase64(String signatureBase64) {
        this.signatureBase64 = signatureBase64;
    }

    public String getTimestampBase64() {
        return timestampBase64;
    }

    public void setTimestampBase64(String timestampBase64) {
        this.timestampBase64 = timestampBase64;
    }

    public String getSigningKeyAlias() {
        return signingKeyAlias;
    }

    public void setSigningKeyAlias(String signingKeyAlias) {
        this.signingKeyAlias = signingKeyAlias;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getCertificateSerial() {
        return certificateSerial;
    }

    public void setCertificateSerial(String certificateSerial) {
        this.certificateSerial = certificateSerial;
    }

    // === File Metadata Getters/Setters ===

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    // === Encryption Field Getters/Setters ===

    public String getEncryptionIv() {
        return encryptionIv;
    }

    public void setEncryptionIv(String encryptionIv) {
        this.encryptionIv = encryptionIv;
    }

    public String getWrappedKey() {
        return wrappedKey;
    }

    public void setWrappedKey(String wrappedKey) {
        this.wrappedKey = wrappedKey;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    // === Countersign Field Getters/Setters ===

    public UUID getAssignedCountersignerId() {
        return assignedCountersignerId;
    }

    public void setAssignedCountersignerId(UUID assignedCountersignerId) {
        this.assignedCountersignerId = assignedCountersignerId;
    }

    public UUID getCountersignatureId() {
        return countersignatureId;
    }

    public void setCountersignatureId(UUID countersignatureId) {
        this.countersignatureId = countersignatureId;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
