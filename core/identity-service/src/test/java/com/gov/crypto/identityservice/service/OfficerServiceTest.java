package com.gov.crypto.identityservice.service;

import com.gov.crypto.model.OfficerAssignment;
import com.gov.crypto.model.Role;
import com.gov.crypto.model.User;
import com.gov.crypto.repository.OfficerAssignmentRepository;
import com.gov.crypto.repository.RoleRepository;
import com.gov.crypto.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OfficerService.
 */
@ExtendWith(MockitoExtension.class)
class OfficerServiceTest {

    @Mock
    private OfficerAssignmentRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private OfficerService officerService;

    private User user;
    private User assigningOfficer;
    private Role policyOfficerRole;
    private Role issuingOfficerRole;
    private Role raOfficerRole;
    private UUID caId;

    @BeforeEach
    void setUp() {
        caId = UUID.randomUUID();

        // Setup roles with hierarchy
        policyOfficerRole = new Role();
        policyOfficerRole.setName("POLICY_OFFICER");
        policyOfficerRole.setHierarchyLevel(0);
        policyOfficerRole.setOfficerRole(true);

        issuingOfficerRole = new Role();
        issuingOfficerRole.setName("ISSUING_OFFICER");
        issuingOfficerRole.setHierarchyLevel(1);
        issuingOfficerRole.setOfficerRole(true);

        raOfficerRole = new Role();
        raOfficerRole.setName("RA_OFFICER");
        raOfficerRole.setHierarchyLevel(2);
        raOfficerRole.setOfficerRole(true);

        // Setup test user
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("officer1");

        // Setup assigning officer with POLICY_OFFICER role
        assigningOfficer = new User();
        assigningOfficer.setId(UUID.randomUUID());
        assigningOfficer.setUsername("policy_officer");
        assigningOfficer.setRole(policyOfficerRole);
    }

    @Nested
    @DisplayName("Officer Check Tests")
    class OfficerCheckTests {

        @Test
        @DisplayName("Should check if user is officer for CA")
        void shouldCheckIfOfficerForCa() {
            // Given
            when(assignmentRepository.existsByOfficerAndCaIdAndActiveTrue(user, caId)).thenReturn(true);

            // When
            boolean result = officerService.isOfficerFor(user, caId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when not officer for CA")
        void shouldReturnFalseWhenNotOfficer() {
            // Given
            when(assignmentRepository.existsByOfficerAndCaIdAndActiveTrue(user, caId)).thenReturn(false);

            // When
            boolean result = officerService.isOfficerFor(user, caId);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should get managed CAs for officer")
        void shouldGetManagedCas() {
            // Given
            when(assignmentRepository.findManagedCaIdsByOfficer(user)).thenReturn(List.of(caId));

            // When
            List<UUID> managedCas = officerService.getManagedCas(user);

            // Then
            assertEquals(1, managedCas.size());
            assertEquals(caId, managedCas.get(0));
        }
    }

    @Nested
    @DisplayName("Stamp Permission Tests")
    class StampPermissionTests {

        @Test
        @DisplayName("Officer with role can apply stamp")
        void officerWithRoleCanApplyStamp() {
            // Given
            user.setRole(raOfficerRole);

            // When
            boolean result = officerService.canApplyStamp(user);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("User without officer role cannot apply stamp")
        void userWithoutOfficerRoleCannotApplyStamp() {
            // Given
            Role citizenRole = new Role();
            citizenRole.setName("CITIZEN");
            citizenRole.setOfficerRole(false);
            user.setRole(citizenRole);

            // When
            boolean result = officerService.canApplyStamp(user);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("User with null role cannot apply stamp")
        void userWithNullRoleCannotApplyStamp() {
            // Given
            user.setRole(null);

            // When
            boolean result = officerService.canApplyStamp(user);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Role Hierarchy Tests")
    class HierarchyTests {

        @Test
        @DisplayName("Policy Officer should have authority over Issuing Officer")
        void policyOfficerShouldHaveAuthorityOverIssuing() {
            assertTrue(policyOfficerRole.hasAuthorityOver(issuingOfficerRole));
        }

        @Test
        @DisplayName("Issuing Officer should have authority over RA Officer")
        void issuingOfficerShouldHaveAuthorityOverRa() {
            assertTrue(issuingOfficerRole.hasAuthorityOver(raOfficerRole));
        }

        @Test
        @DisplayName("RA Officer should not have authority over Issuing Officer")
        void raOfficerShouldNotHaveAuthorityOverIssuing() {
            assertFalse(raOfficerRole.hasAuthorityOver(issuingOfficerRole));
        }

        @Test
        @DisplayName("Same level should have equal authority")
        void sameLevelShouldHaveEqualAuthority() {
            // Per the implementation: hasAuthorityOver checks for "higher or equal
            // authority"
            // Same level counts as equal, so it returns true
            assertTrue(issuingOfficerRole.hasAuthorityOver(issuingOfficerRole));
        }
    }
}
