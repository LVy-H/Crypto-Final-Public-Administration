package com.gov.crypto.org.repository;

import com.gov.crypto.org.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByCode(String code);

    List<Organization> findByParentId(UUID parentId);

    List<Organization> findByParentIdIsNull();

    @Query("SELECT o FROM Organization o WHERE o.parentId = :parentId OR o.id = :parentId")
    List<Organization> findOrgAndChildren(UUID parentId);

    @Query(value = """
            WITH RECURSIVE org_tree AS (
                SELECT * FROM organizations WHERE id = :orgId
                UNION ALL
                SELECT o.* FROM organizations o
                INNER JOIN org_tree t ON o.parent_id = t.id
            )
            SELECT * FROM org_tree
            """, nativeQuery = true)
    List<Organization> findAllDescendants(UUID orgId);
}
