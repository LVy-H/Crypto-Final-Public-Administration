package com.gov.crypto.caauthority.repository;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateAuthorityRepository extends JpaRepository<CertificateAuthority, UUID> {

    Optional<CertificateAuthority> findByHierarchyLevelAndStatus(int hierarchyLevel,
            CertificateAuthority.CaStatus status);

    List<CertificateAuthority> findByParentCa(CertificateAuthority parentCa);

    List<CertificateAuthority> findByHierarchyLevel(int hierarchyLevel);

    Optional<CertificateAuthority> findByName(String name);

    // Find Internal CA by label
    List<CertificateAuthority> findByLabel(String label);

    // Find CA linked to a specific organization
    Optional<CertificateAuthority> findByOrganizationId(UUID organizationId);
}
