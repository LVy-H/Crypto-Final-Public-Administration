package com.gov.crypto.caauthority.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificate_authorities")
public class CertificateAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CaType type; // ISSUING_CA, RA

    @Column(nullable = false)
    private int hierarchyLevel; // 0=Root, 1=Provincial, 2=District/Internal

    @Column(nullable = false)
    private String label; // "Root CA", "Provincial CA", "District RA"

    @ManyToOne
    @JoinColumn(name = "parent_ca_id")
    private CertificateAuthority parentCa;

    @Column(nullable = false)
    private String algorithm; // ML-DSA-87, ML-DSA-65, ECDSA-P384

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @Column(columnDefinition = "TEXT")
    private String certificate;

    @Column(columnDefinition = "TEXT")
    private String privateKeyPath; // Path to encrypted key file

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    private CaStatus status; // ACTIVE, REVOKED, EXPIRED

    private String subjectDn;

    @Column(name = "province")
    private String province;

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

    public CaType getType() {
        return type;
    }

    public void setType(CaType type) {
        this.type = type;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    public void setHierarchyLevel(int hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public CertificateAuthority getParentCa() {
        return parentCa;
    }

    public void setParentCa(CertificateAuthority parentCa) {
        this.parentCa = parentCa;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
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

    public CaStatus getStatus() {
        return status;
    }

    public void setStatus(CaStatus status) {
        this.status = status;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public enum CaType {
        ISSUING_CA, // Can issue to other CAs (if policy allows)
        RA, // Can only issue end-entity certificates (identities/docs)
        EXTERNAL_RA // Third-party RA managing their own keys
    }

    public enum CaStatus {
        ACTIVE, REVOKED, EXPIRED
    }
}
