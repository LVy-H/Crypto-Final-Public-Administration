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

    private String role; // CITIZEN, OFFICIAL, ADMIN

    @Enumerated(EnumType.STRING)
    private IdentityStatus identityStatus; // UNVERIFIED, PENDING, VERIFIED, REJECTED

    private String identityDocumentSignature; // Signature of the verified identity document

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
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
}
