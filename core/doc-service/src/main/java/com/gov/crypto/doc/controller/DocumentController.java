package com.gov.crypto.doc.controller;

import com.gov.crypto.doc.dto.DocumentDto;
import com.gov.crypto.doc.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doc")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping("/public")
    public List<DocumentDto> getPublicDocuments() {
        return service.getPublicDocuments();
    }

    @GetMapping("/my")
    public List<DocumentDto> getMyDocuments(@RequestHeader("X-User-Id") UUID userId) {
        return service.getByOwner(userId);
    }

    @GetMapping("/org/{orgId}")
    public List<DocumentDto> getOrgDocuments(@PathVariable UUID orgId) {
        return service.getByOrg(orgId);
    }

    @GetMapping("/{id}")
    public DocumentDto getById(@PathVariable UUID id) {
        return service.getById(id);
    }
}
