package com.gov.crypto.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Enumerated(EnumType.STRING)
    private IdentityStatus identityStatus; // UNVERIFIED, PENDING, VERIFIED, REJECTED

    private String identityDocumentSignature; // Signature of the verified identity document

    // ABAC Jurisdiction Fields
    @Column(name = "ra_id")
    private UUID raId; // For RA_OPERATOR, restricts scope to this RA

    @Column(name = "org_id")
    private UUID orgId; // For ORGANIZATION, restricts scope to this Org

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public IdentityStatus getIdentityStatus() {
        return identityStatus;
    }

    public void setIdentityStatus(IdentityStatus identityStatus) {
        this.identityStatus = identityStatus;
    }

    public String getIdentityDocumentSignature() {
        return identityDocumentSignature;
    }

    public void setIdentityDocumentSignature(String identityDocumentSignature) {
        this.identityDocumentSignature = identityDocumentSignature;
    }

    public enum IdentityStatus {
        UNVERIFIED, PENDING, VERIFIED, REJECTED
    }

    public UUID getRaId() {
        return raId;
    }

    public void setRaId(UUID raId) {
        this.raId = raId;
    }

    @Column(name = "assigned_ca_id")
    private UUID assignedCaId;

    @Column(name = "province")
    private String province; // For ABAC scoping

    public UUID getAssignedCaId() {
        return assignedCaId;
    }

    public void setAssignedCaId(UUID assignedCaId) {
        this.assignedCaId = assignedCaId;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }
}
