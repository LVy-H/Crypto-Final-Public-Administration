package com.gov.crypto.caauthority.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a countersignature (stamp) applied by an officer to legitimize
 * a user-signed document.
 */
@Entity
@Table(name = "countersignatures")
public class Countersignature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The hash of the original document.
     */
    @Column(name = "document_hash", nullable = false, length = 128)
    private String documentHash;

    /**
     * The user's signature on the document (Base64 encoded).
     */
    @Column(name = "user_signature", nullable = false, columnDefinition = "TEXT")
    private String userSignature;

    /**
     * The user's certificate PEM used for signing.
     */
    @Column(name = "user_cert_pem", nullable = false, columnDefinition = "TEXT")
    private String userCertPem;

    /**
     * The officer's countersignature (Base64 encoded).
     * Signs: documentHash + userSignature
     */
    @Column(name = "officer_signature", nullable = false, columnDefinition = "TEXT")
    private String officerSignature;

    /**
     * The officer's certificate PEM.
     */
    @Column(name = "officer_cert_pem", nullable = false, columnDefinition = "TEXT")
    private String officerCertPem;

    /**
     * UUID of the officer who applied the stamp.
     */
    @Column(name = "officer_id", nullable = false)
    private UUID officerId;

    /**
     * RFC 3161 timestamp token (Base64 encoded).
     */
    @Column(name = "timestamp_token", columnDefinition = "TEXT")
    private String timestampToken;

    /**
     * When the stamp was applied.
     */
    @Column(name = "stamped_at", nullable = false)
    private Instant stampedAt;

    /**
     * Purpose of the stamp.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stamp_purpose", nullable = false)
    private StampPurpose purpose = StampPurpose.OFFICIAL_VALIDATION;

    /**
     * Status of the countersignature.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    public enum StampPurpose {
        OFFICIAL_VALIDATION, // Standard document validation
        NOTARIZATION, // Notarized document
        APPROVAL, // Approval signature
        CERTIFICATION // Certificate of authenticity
    }

    public enum Status {
        ACTIVE,
        REVOKED,
        EXPIRED
    }

    public Countersignature() {
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public String getUserSignature() {
        return userSignature;
    }

    public void setUserSignature(String userSignature) {
        this.userSignature = userSignature;
    }

    public String getUserCertPem() {
        return userCertPem;
    }

    public void setUserCertPem(String userCertPem) {
        this.userCertPem = userCertPem;
    }

    public String getOfficerSignature() {
        return officerSignature;
    }

    public void setOfficerSignature(String officerSignature) {
        this.officerSignature = officerSignature;
    }

    public String getOfficerCertPem() {
        return officerCertPem;
    }

    public void setOfficerCertPem(String officerCertPem) {
        this.officerCertPem = officerCertPem;
    }

    public UUID getOfficerId() {
        return officerId;
    }

    public void setOfficerId(UUID officerId) {
        this.officerId = officerId;
    }

    public String getTimestampToken() {
        return timestampToken;
    }

    public void setTimestampToken(String timestampToken) {
        this.timestampToken = timestampToken;
    }

    public Instant getStampedAt() {
        return stampedAt;
    }

    public void setStampedAt(Instant stampedAt) {
        this.stampedAt = stampedAt;
    }

    public StampPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(StampPurpose purpose) {
        this.purpose = purpose;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
