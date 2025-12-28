package com.gov.crypto.repository;

import com.gov.crypto.model.OfficerAssignment;
import com.gov.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfficerAssignmentRepository extends JpaRepository<OfficerAssignment, UUID> {

    /**
     * Find all active assignments for an officer.
     */
    List<OfficerAssignment> findByOfficerAndActiveTrue(User officer);

    /**
     * Find assignment for a specific officer and CA.
     */
    Optional<OfficerAssignment> findByOfficerAndCaIdAndActiveTrue(User officer, UUID caId);

    /**
     * Find all officers assigned to a specific CA.
     */
    List<OfficerAssignment> findByCaIdAndActiveTrue(UUID caId);

    /**
     * Check if a user is an officer for a specific CA.
     */
    boolean existsByOfficerAndCaIdAndActiveTrue(User officer, UUID caId);

    /**
     * Find all assignments by CA type for a user.
     */
    List<OfficerAssignment> findByOfficerAndCaTypeAndActiveTrue(User officer, OfficerAssignment.CaType caType);

    /**
     * Get all CAs managed by an officer (just the IDs).
     */
    @Query("SELECT oa.caId FROM OfficerAssignment oa WHERE oa.officer = :officer AND oa.active = true")
    List<UUID> findManagedCaIdsByOfficer(@Param("officer") User officer);
}
