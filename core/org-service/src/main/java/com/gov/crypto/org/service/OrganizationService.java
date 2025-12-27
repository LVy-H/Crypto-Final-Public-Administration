package com.gov.crypto.org.service;

import com.gov.crypto.org.dto.CreateOrganizationRequest;
import com.gov.crypto.org.dto.OrganizationDto;
import com.gov.crypto.org.entity.Organization;
import com.gov.crypto.org.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    public List<OrganizationDto> getAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public OrganizationDto getById(UUID id) {
        return repository.findById(id).map(this::toDto).orElseThrow();
    }

    public OrganizationDto getByCode(String code) {
        return repository.findByCode(code).map(this::toDto).orElseThrow();
    }

    public List<OrganizationDto> getRootOrganizations() {
        return repository.findByParentIdIsNull().stream().map(this::toDto).toList();
    }

    public List<OrganizationDto> getChildren(UUID parentId) {
        return repository.findByParentId(parentId).stream().map(this::toDto).toList();
    }

    public List<OrganizationDto> getDescendants(UUID orgId) {
        return repository.findAllDescendants(orgId).stream().map(this::toDto).toList();
    }

    @Transactional
    public OrganizationDto create(CreateOrganizationRequest request) {
        Organization org = new Organization();
        org.setName(request.name());
        org.setCode(request.code());
        org.setParentId(request.parentId());
        org.setType(request.type());

        // Calculate level based on parent
        if (request.parentId() == null) {
            org.setLevel(0);
        } else {
            Organization parent = repository.findById(request.parentId()).orElseThrow();
            org.setLevel(parent.getLevel() + 1);
        }

        return toDto(repository.save(org));
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private OrganizationDto toDto(Organization org) {
        return new OrganizationDto(
                org.getId(),
                org.getName(),
                org.getCode(),
                org.getParentId(),
                org.getType(),
                org.getLevel());
    }
}
