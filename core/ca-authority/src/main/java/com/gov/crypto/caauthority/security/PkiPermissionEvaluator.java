package com.gov.crypto.caauthority.security;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.common.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class PkiPermissionEvaluator implements PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PkiPermissionEvaluator.class);

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if ((auth == null) || (targetDomainObject == null) || !(permission instanceof String)) {
            return false;
        }

        if (targetDomainObject instanceof CertificateAuthority ca) {
            return checkCaAccess(auth, ca, (String) permission);
        }

        // Add other domain objects (e.g. CaPendingRequest) here

        // Fallback to basic role check if object type is unknown but permission is
        // generic
        String targetType = targetDomainObject.getClass().getSimpleName().toUpperCase();
        return hasPrivilege(auth, targetType, permission.toString().toUpperCase());
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        // ID-based lookup would require Repository injection here.
        // For efficiency, we usually use the object-based check in Controller.
        // If absolutely needed, inject CertificateAuthorityRepository.
        return false;
    }

    private boolean checkCaAccess(Authentication auth, CertificateAuthority ca, String permission) {
        UserPrincipal user = getUserPrincipal(auth);
        if (user == null)
            return false;

        // 1. National Admins/Policy Officers can do anything
        if (hasRole(auth, "ROLE_NATIONAL_ADMIN") || hasRole(auth, "ROLE_POLICY_OFFICER")) {
            return true;
        }

        // 2. Issuing Officers (Provincial)
        if (hasRole(auth, "ROLE_ISSUING_OFFICER")) {
            // Scope: Can MANAGE/ISSUE only if CA matches their province
            boolean provinceMatch = user.getProvince() != null && user.getProvince().equalsIgnoreCase(ca.getProvince());

            // Allow managing the Provincial CA itself, or its children (District RAs)
            // Note: If ca is District RA, its province should match parent's province.
            // Assumption: District RA record also carries 'province' field or we check
            // logic.
            // Simplification: We blindly check 'province' field on CA entity.
            return provinceMatch;
        }

        // 3. RA Operators (District)
        if (hasRole(auth, "ROLE_RA_OPERATOR")) {
            // Scope: Can only VIEW or ISSUE_USER_CERT on their assigned RA
            if ("ISSUE_USER_CERT".equals(permission) || "VIEW".equals(permission)) {
                return user.getAssignedCaId() != null && user.getAssignedCaId().equals(ca.getId());
            }
        }

        return false;
    }

    private boolean hasPrivilege(Authentication auth, String targetType, String permission) {
        if (hasRole(auth, "ROLE_NATIONAL_ADMIN"))
            return true;
        // ... (keep legacy simplistic logic if needed as fallback)
        return false;
    }

    private boolean hasRole(Authentication auth, String role) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(role))
                return true;
        }
        return false;
    }

    private UserPrincipal getUserPrincipal(Authentication auth) {
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null; // Anonymous or incompatible principal
    }
}
