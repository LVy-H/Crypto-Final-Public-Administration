package com.gov.crypto.pkiservice.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * CSR Request entity for offline CA workflow.
 * 
 * Workflow:
 * 1. User submits CSR → status = PENDING
 * 2. CA Operator downloads pending CSRs
 * 3. CA Operator signs offline with CA private key
 * 4. CA Operator uploads signed certificate → status = SIGNED
 * 5. User retrieves certificate
 */
@Entity
@Table(name = "csr_requests")
data class CsrRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: String, // Username of the requestor

    @Column(nullable = false, length = 10000)
    val csrPem: String, // Base64 encoded CSR

    @Column(nullable = false)
    val subjectDn: String, // Extracted from CSR

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CsrStatus = CsrStatus.PENDING,

    @Column(length = 50000)
    var signedCertificate: String? = null, // Base64 encoded X.509 when signed

    @Column
    var rejectionReason: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column
    var processedAt: Instant? = null,

    @Column
    var processedBy: String? = null // CA Operator who processed
)

enum class CsrStatus {
    PENDING,   // Waiting for CA Operator
    SIGNED,    // Certificate issued
    REJECTED   // CSR rejected
}
