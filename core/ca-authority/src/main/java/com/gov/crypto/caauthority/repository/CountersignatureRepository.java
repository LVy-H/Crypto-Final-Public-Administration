package com.gov.crypto.caauthority.repository;

import com.gov.crypto.caauthority.model.Countersignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountersignatureRepository extends JpaRepository<Countersignature, UUID> {

    /**
     * Find countersignature by document hash.
     */
    List<Countersignature> findByDocumentHash(String documentHash);

    /**
     * Find active countersignatures by document hash.
     */
    List<Countersignature> findByDocumentHashAndStatus(String documentHash, Countersignature.Status status);

    /**
     * Find all stamps by an officer.
     */
    List<Countersignature> findByOfficerId(UUID officerId);

    /**
     * Find stamp by document hash and user signature (unique combination).
     */
    Optional<Countersignature> findByDocumentHashAndUserSignature(String documentHash, String userSignature);

    /**
     * Check if a document+signature combination has already been stamped.
     */
    boolean existsByDocumentHashAndUserSignatureAndStatus(String documentHash, String userSignature,
            Countersignature.Status status);
}
