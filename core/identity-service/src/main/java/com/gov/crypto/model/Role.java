package com.gov.crypto.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., ADMIN, CITIZEN

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "roles_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Hierarchy level for officer roles.
     * null = non-officer (e.g., CITIZEN)
     * 0 = highest online officer (POLICY_OFFICER)
     * 1 = mid-level officer (ISSUING_OFFICER)
     * 2 = lowest officer (RA_OFFICER)
     */
    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel;

    /**
     * Whether this role is an officer role that can manage CAs/RAs.
     */
    @Column(name = "is_officer_role", nullable = false)
    private boolean officerRole = false;

    public Role() {
    }

    public Role(String name) {
        this.name = name;
    }

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

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    public void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    public boolean isOfficerRole() {
        return officerRole;
    }

    public void setOfficerRole(boolean officerRole) {
        this.officerRole = officerRole;
    }

    /**
     * Check if this role has higher or equal authority than another role.
     * Lower hierarchy level = higher authority.
     */
    public boolean hasAuthorityOver(Role other) {
        if (!this.officerRole)
            return false;
        if (!other.officerRole)
            return true; // Officers have authority over non-officers
        if (this.hierarchyLevel == null || other.hierarchyLevel == null)
            return false;
        return this.hierarchyLevel <= other.hierarchyLevel;
    }
}
