package com.gov.crypto.identityservice.config;

import com.gov.crypto.model.Permission;
import com.gov.crypto.model.Role;
import com.gov.crypto.repository.PermissionRepository;
import com.gov.crypto.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

        @Bean
        CommandLineRunner initDatabase(RoleRepository roleRepository, PermissionRepository permissionRepository) {
                return args -> {
                        seedData(roleRepository, permissionRepository);
                };
        }

        @Transactional
        public void seedData(RoleRepository roleRepository, PermissionRepository permissionRepository) {
                // Permissions
                Permission userRead = createPermissionIfNotFound(permissionRepository, "USER_READ",
                                "Read user details");
                Permission userWrite = createPermissionIfNotFound(permissionRepository, "USER_WRITE",
                                "Modify user details");
                Permission adminAccess = createPermissionIfNotFound(permissionRepository, "ADMIN_ACCESS",
                                "Access admin features");
                Permission verifyIdentity = createPermissionIfNotFound(permissionRepository, "VERIFY_IDENTITY",
                                "Verify user identity");

                // New officer permissions
                Permission manageCA = createPermissionIfNotFound(permissionRepository, "MANAGE_CA",
                                "Create and manage Certificate Authorities");
                Permission manageRA = createPermissionIfNotFound(permissionRepository, "MANAGE_RA",
                                "Create and manage Registration Authorities");
                Permission issueCert = createPermissionIfNotFound(permissionRepository, "ISSUE_CERT",
                                "Issue certificates to users");
                Permission applyStamp = createPermissionIfNotFound(permissionRepository, "APPLY_STAMP",
                                "Apply countersignature/stamp to documents");
                Permission assignOfficer = createPermissionIfNotFound(permissionRepository, "ASSIGN_OFFICER",
                                "Assign officer roles to users");

                // Non-officer roles
                createOrUpdateRole(roleRepository, "ADMIN",
                                Set.of(userRead, userWrite, adminAccess, verifyIdentity, manageCA, manageRA, issueCert,
                                                applyStamp, assignOfficer),
                                null, false);
                createOrUpdateRole(roleRepository, "OFFICIAL",
                                Set.of(userRead, verifyIdentity),
                                null, false);
                createOrUpdateRole(roleRepository, "CITIZEN",
                                Set.of(userRead),
                                null, false);

                // Officer roles (with hierarchy)
                // Level 0 = highest authority (POLICY_OFFICER)
                createOrUpdateRole(roleRepository, "POLICY_OFFICER",
                                Set.of(userRead, userWrite, adminAccess, verifyIdentity, manageCA, manageRA, issueCert,
                                                applyStamp,
                                                assignOfficer),
                                0, true);

                // Level 1 = mid-level (ISSUING_OFFICER)
                createOrUpdateRole(roleRepository, "ISSUING_OFFICER",
                                Set.of(userRead, userWrite, verifyIdentity, manageRA, issueCert, applyStamp,
                                                assignOfficer),
                                1, true);

                // Level 2 = lowest officer (RA_OFFICER)
                createOrUpdateRole(roleRepository, "RA_OFFICER",
                                Set.of(userRead, verifyIdentity, issueCert, applyStamp),
                                2, true);
        }

        private Permission createPermissionIfNotFound(PermissionRepository repository, String name,
                        String description) {
                return repository.findByName(name).orElseGet(() -> {
                        Permission permission = new Permission(name, description);
                        return repository.save(permission);
                });
        }

        private Role createOrUpdateRole(RoleRepository repository, String name, Set<Permission> permissions,
                        Integer hierarchyLevel, boolean isOfficerRole) {
                Role role = repository.findByName(name).orElseGet(() -> new Role(name));
                role.setPermissions(permissions);
                role.setHierarchyLevel(hierarchyLevel);
                role.setOfficerRole(isOfficerRole);
                return repository.save(role);
        }
}
