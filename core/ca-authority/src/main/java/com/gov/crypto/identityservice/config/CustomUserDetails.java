package com.gov.crypto.identityservice.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Duplicate of Identity Service's CustomUserDetails to allow session
 * deserialization.
 * MUST MATCH FIELDS EXACTLY.
 */
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 7123456789L;

    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UUID raId;
    private final UUID orgId;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
            UUID raId, UUID orgId) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.raId = raId;
        this.orgId = orgId;
    }

    public UUID getRaId() {
        return raId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
