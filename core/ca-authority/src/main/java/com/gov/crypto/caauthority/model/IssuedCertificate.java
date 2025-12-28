package com.gov.crypto.caauthority.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "issued_certificates")
public class IssuedCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "issuing_ca_id", nullable = false)
    private CertificateAuthority issuingCa;

    @Column(nullable = false)
    private String subjectDn;

    @Column(unique = true, nullable = false)
    private String serialNumber;

    @Column(name = "username")
    private String username;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String certificate;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    private CertStatus status; // ACTIVE, REVOKED, EXPIRED

    private LocalDateTime revokedAt;
    private String revocationReason;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CertificateAuthority getIssuingCa() {
        return issuingCa;
    }

    public void setIssuingCa(CertificateAuthority issuingCa) {
        this.issuingCa = issuingCa;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public CertStatus getStatus() {
        return status;
    }

    public void setStatus(CertStatus status) {
        this.status = status;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public enum CertStatus {
        ACTIVE, REVOKED, EXPIRED, PENDING
    }
}
