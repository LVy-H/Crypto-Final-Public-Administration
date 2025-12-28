package com.gov.crypto.identityservice.service;

import com.gov.crypto.model.OfficerAssignment;
import com.gov.crypto.model.Role;
import com.gov.crypto.model.User;
import com.gov.crypto.repository.OfficerAssignmentRepository;
import com.gov.crypto.repository.RoleRepository;
import com.gov.crypto.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing officer roles and CA assignments.
 */
@Service
public class OfficerService {

    private final OfficerAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public OfficerService(OfficerAssignmentRepository assignmentRepository,
            RoleRepository roleRepository,
            UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Assign an officer to manage a specific CA.
     *
     * @param officerId    The user to become an officer
     * @param caId         The CA/RA they will manage
     * @param caType       Type of CA
     * @param assignedById The officer making this assignment (must have higher
     *                     authority)
     */
    @Transactional
    public OfficerAssignment assignOfficer(UUID officerId, UUID caId, OfficerAssignment.CaType caType,
            UUID assignedById) {
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + officerId));

        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new IllegalArgumentException("Assigning user not found: " + assignedById));

        // Verify assigning user has authority
        if (!canAssignOfficer(assignedBy, caType)) {
            throw new SecurityException("User does not have authority to assign " + caType + " officers");
        }

        // Check if already assigned
        if (assignmentRepository.existsByOfficerAndCaIdAndActiveTrue(officer, caId)) {
            throw new IllegalStateException("User is already assigned to this CA");
        }

        // Update user's role if needed
        Role requiredRole = getRequiredRole(caType);
        if (officer.getRole() == null || !officer.getRole().isOfficerRole() ||
                officer.getRole().getHierarchyLevel() > requiredRole.getHierarchyLevel()) {
            officer.setRole(requiredRole);
            userRepository.save(officer);
        }

        OfficerAssignment assignment = new OfficerAssignment(officer, caId, caType, assignedBy);
        return assignmentRepository.save(assignment);
    }

    /**
     * Revoke an officer's assignment.
     */
    @Transactional
    public void revokeAssignment(UUID assignmentId, UUID revokedById) {
        OfficerAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        User revokedBy = userRepository.findById(revokedById)
                .orElseThrow(() -> new IllegalArgumentException("Revoking user not found: " + revokedById));

        // Verify revoking user has authority
        if (!canAssignOfficer(revokedBy, assignment.getCaType())) {
            throw new SecurityException("User does not have authority to revoke this assignment");
        }

        assignment.setActive(false);
        assignmentRepository.save(assignment);
    }

    /**
     * Check if a user can apply countersignatures (stamps).
     * Only RA_OFFICER and higher can stamp.
     */
    public boolean canApplyStamp(User user) {
        if (user.getRole() == null || !user.getRole().isOfficerRole()) {
            return false;
        }
        // All officer levels can stamp, but in practice RA_OFFICER does it most
        return user.getRole().getHierarchyLevel() != null;
    }

    /**
     * Check if a user is an officer for a specific CA.
     */
    public boolean isOfficerFor(User user, UUID caId) {
        return assignmentRepository.existsByOfficerAndCaIdAndActiveTrue(user, caId);
    }

    /**
     * Get all CAs managed by an officer.
     */
    public List<UUID> getManagedCas(User officer) {
        return assignmentRepository.findManagedCaIdsByOfficer(officer);
    }

    /**
     * Get all assignments for an officer.
     */
    public List<OfficerAssignment> getOfficerAssignments(User officer) {
        return assignmentRepository.findByOfficerAndActiveTrue(officer);
    }

    /**
     * Check if a user can assign officers of a specific type.
     */
    private boolean canAssignOfficer(User user, OfficerAssignment.CaType caType) {
        if (user.getRole() == null || !user.getRole().isOfficerRole()) {
            return false;
        }

        Integer userLevel = user.getRole().getHierarchyLevel();
        if (userLevel == null)
            return false;

        // Officers can only assign officers at lower levels
        return switch (caType) {
            case POLICY_CA -> userLevel == 0; // Only POLICY_OFFICER can assign POLICY_CA officers (themselves)
            case ISSUING_CA -> userLevel <= 0; // POLICY_OFFICER can assign ISSUING_CA officers
            case REGISTRATION_AUTHORITY -> userLevel <= 1; // POLICY or ISSUING officers can assign RA officers
        };
    }

    /**
     * Get the required role for a CA type.
     */
    private Role getRequiredRole(OfficerAssignment.CaType caType) {
        String roleName = switch (caType) {
            case POLICY_CA -> "POLICY_OFFICER";
            case ISSUING_CA -> "ISSUING_OFFICER";
            case REGISTRATION_AUTHORITY -> "RA_OFFICER";
        };

        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
    }
}
