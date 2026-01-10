package com.gov.crypto.identityservice.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * User entity with RBAC support.
 * 
 * Roles:
 * - USER: Default role, can submit CSRs and get own certificates
 * - CA_OPERATOR: Can manage CSR queue (approve/reject, upload signed certs)
 * - ADMIN: Full system access including user management
 */
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val username: String,

    @Column(nullable = false)
    val passwordHash: String, // BCrypt

    @Column(nullable = false)
    val checkSum: String, // Integrity check for PQC compliance

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "role")
    val roles: Set<String> = setOf("USER"),

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)

/**
 * Available roles in the system.
 */
object Roles {
    const val USER = "USER"
    const val CA_OPERATOR = "CA_OPERATOR"
    const val ADMIN = "ADMIN"
}
