package com.gov.crypto.org.dto;

import com.gov.crypto.org.entity.OrganizationType;
import java.util.UUID;

public record CreateOrganizationRequest(
        String name,
        String code,
        UUID parentId,
        OrganizationType type) {
}
