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
        Permission userRead = createPermissionIfNotFound(permissionRepository, "USER_READ", "Read user details");
        Permission userWrite = createPermissionIfNotFound(permissionRepository, "USER_WRITE", "Modify user details");
        Permission adminAccess = createPermissionIfNotFound(permissionRepository, "ADMIN_ACCESS",
                "Access admin features");
        Permission verifyIdentity = createPermissionIfNotFound(permissionRepository, "VERIFY_IDENTITY",
                "Verify user identity");

        // Roles
        createRoleIfNotFound(roleRepository, "ADMIN", Set.of(userRead, userWrite, adminAccess, verifyIdentity));
        createRoleIfNotFound(roleRepository, "OFFICIAL", Set.of(userRead, verifyIdentity));
        createRoleIfNotFound(roleRepository, "CITIZEN", Set.of(userRead));
    }

    private Permission createPermissionIfNotFound(PermissionRepository repository, String name, String description) {
        return repository.findByName(name).orElseGet(() -> {
            Permission permission = new Permission(name, description);
            return repository.save(permission);
        });
    }

    private Role createRoleIfNotFound(RoleRepository repository, String name, Set<Permission> permissions) {
        return repository.findByName(name).orElseGet(() -> {
            Role role = new Role(name);
            role.setPermissions(permissions);
            return repository.save(role);
        });
    }
}
