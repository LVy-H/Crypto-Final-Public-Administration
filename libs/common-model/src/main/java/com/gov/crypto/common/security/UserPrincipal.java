package com.gov.crypto.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

public class UserPrincipal extends User {

    private final UUID id;
    private final String province;
    private final UUID assignedCaId;
    private final String identityStatus;

    public UserPrincipal(UUID id, String username, String password, Collection<? extends GrantedAuthority> authorities,
            String province, UUID assignedCaId, String identityStatus) {
        super(username, password, authorities);
        this.id = id;
        this.province = province;
        this.assignedCaId = assignedCaId;
        this.identityStatus = identityStatus;
    }

    public UUID getId() {
        return id;
    }

    public String getProvince() {
        return province;
    }

    public UUID getAssignedCaId() {
        return assignedCaId;
    }

    public String getIdentityStatus() {
        return identityStatus;
    }
}
