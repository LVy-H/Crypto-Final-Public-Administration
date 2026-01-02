package com.gov.crypto.caauthority.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ca_pending_requests")
public class CaPendingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String algorithm;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String csrPem;

    @Column(nullable = false)
    private String privateKeyPath; // Encrypted path for non-repudiation (only requester can init, but system
                                   // stores it)

    @ManyToOne(optional = true)
    @JoinColumn(name = "parent_ca_id")
    private CertificateAuthority parentCa;

    @Column(nullable = false)
    private String requestedBy; // Username

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    private String processedBy; // Username
    private String rejectionReason;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    // Getters and Setters

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

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getCsrPem() {
        return csrPem;
    }

    public void setCsrPem(String csrPem) {
        this.csrPem = csrPem;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public CertificateAuthority getParentCa() {
        return parentCa;
    }

    public void setParentCa(CertificateAuthority parentCa) {
        this.parentCa = parentCa;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
