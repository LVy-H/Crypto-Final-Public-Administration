package com.gov.crypto.org.controller;

import com.gov.crypto.org.dto.CreateOrganizationRequest;
import com.gov.crypto.org.dto.OrganizationDto;
import com.gov.crypto.org.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizationDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/roots")
    public List<OrganizationDto> getRoots() {
        return service.getRootOrganizations();
    }

    @GetMapping("/{id}")
    public OrganizationDto getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/code/{code}")
    public OrganizationDto getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @GetMapping("/{id}/children")
    public List<OrganizationDto> getChildren(@PathVariable UUID id) {
        return service.getChildren(id);
    }

    @GetMapping("/{id}/descendants")
    public List<OrganizationDto> getDescendants(@PathVariable UUID id) {
        return service.getDescendants(id);
    }

    @PostMapping
    public ResponseEntity<OrganizationDto> create(@RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
