package com.gov.crypto.doc.repository;

import com.gov.crypto.doc.entity.Document;
import com.gov.crypto.doc.entity.DocumentClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwnerId(UUID ownerId);

    List<Document> findByOrgId(UUID orgId);

    List<Document> findByClassification(DocumentClassification classification);

    @Query("SELECT d FROM Document d WHERE d.classification = 'PUBLIC'")
    List<Document> findPublicDocuments();

    @Query("SELECT d FROM Document d WHERE d.ownerId = :userId OR d.classification = 'PUBLIC'")
    List<Document> findAccessibleByUser(UUID userId);

    @Query("SELECT d FROM Document d WHERE d.orgId IN :orgIds OR d.classification = 'PUBLIC'")
    List<Document> findAccessibleByOrgs(List<UUID> orgIds);
}
