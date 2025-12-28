package com.gov.crypto.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Links an officer (user with officer role) to a specific CA or RA they manage.
 * An officer can manage multiple CAs/RAs if they have appropriate authority.
 */
@Entity
@Table(name = "officer_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "officer_id", "ca_id" })
})
public class OfficerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;

    /**
     * The CA or RA UUID that this officer manages.
     * References ca-authority's HierarchicalCa.id
     */
    @Column(name = "ca_id", nullable = false)
    private UUID caId;

    /**
     * Type of CA this officer manages.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ca_type", nullable = false)
    private CaType caType;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public enum CaType {
        POLICY_CA, // Intermediate/Policy CA (highest online)
        ISSUING_CA, // Issuing CA
        REGISTRATION_AUTHORITY // RA
    }

    public OfficerAssignment() {
    }

    public OfficerAssignment(User officer, UUID caId, CaType caType, User assignedBy) {
        this.officer = officer;
        this.caId = caId;
        this.caType = caType;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
        this.active = true;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getOfficer() {
        return officer;
    }

    public void setOfficer(User officer) {
        this.officer = officer;
    }

    public UUID getCaId() {
        return caId;
    }

    public void setCaId(UUID caId) {
        this.caId = caId;
    }

    public CaType getCaType() {
        return caType;
    }

    public void setCaType(CaType caType) {
        this.caType = caType;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
