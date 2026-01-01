package com.gov.crypto.identityservice.config;

import com.gov.crypto.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 7123456789L;

    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UUID raId;
    private final UUID orgId;

    public CustomUserDetails(User user) {
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.authorities = user.getRole() != null
                ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()))
                : Collections.emptyList();
        this.raId = user.getRaId();
        this.orgId = user.getOrgId();
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
