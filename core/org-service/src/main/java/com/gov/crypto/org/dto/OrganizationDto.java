package com.gov.crypto.org.dto;

import com.gov.crypto.org.entity.OrganizationType;
import java.util.UUID;

public record OrganizationDto(
        UUID id,
        String name,
        String code,
        UUID parentId,
        OrganizationType type,
        int level) {
}
