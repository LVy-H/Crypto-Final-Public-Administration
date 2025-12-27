package com.gov.crypto.doc.service;

import com.gov.crypto.doc.dto.DocumentDto;
import com.gov.crypto.doc.entity.Document;
import com.gov.crypto.doc.entity.DocumentClassification;
import com.gov.crypto.doc.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public List<DocumentDto> getPublicDocuments() {
        return repository.findPublicDocuments().stream().map(this::toDto).toList();
    }

    public List<DocumentDto> getByOwner(UUID ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(this::toDto).toList();
    }

    public List<DocumentDto> getByOrg(UUID orgId) {
        return repository.findByOrgId(orgId).stream().map(this::toDto).toList();
    }

    public List<DocumentDto> getAccessibleByUser(UUID userId) {
        return repository.findAccessibleByUser(userId).stream().map(this::toDto).toList();
    }

    public DocumentDto getById(UUID id) {
        return repository.findById(id).map(this::toDto).orElseThrow();
    }

    private DocumentDto toDto(Document doc) {
        return new DocumentDto(
                doc.getId(),
                doc.getName(),
                doc.getOwnerId(),
                doc.getOrgId(),
                doc.getClassification(),
                doc.getVisibility(),
                doc.getSignatureId(),
                doc.getSignedAt(),
                doc.getCreatedAt());
    }
}
