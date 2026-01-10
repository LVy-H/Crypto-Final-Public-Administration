package com.gov.crypto.pkiservice.controller

import com.gov.crypto.pkiservice.entity.CsrRequest
import com.gov.crypto.pkiservice.entity.CsrStatus
import com.gov.crypto.pkiservice.service.CsrService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Admin Controller for CA Operators.
 * 
 * These endpoints require CA_OPERATOR or ADMIN role.
 * Used for offline CA workflow:
 * - Download pending CSRs
 * - Upload signed certificates
 * - Reject invalid CSRs
 */
@RestController
@RequestMapping("/pki/admin")
class AdminController(private val csrService: CsrService) {

    /**
     * Get all pending CSR requests.
     * CA Operators use this to download CSRs for offline signing.
     */
    @GetMapping("/csr/pending")
    @PreAuthorize("hasAnyRole('CA_OPERATOR', 'ADMIN')")
    fun getPendingCsrs(): ResponseEntity<List<CsrRequestDto>> {
        val pending = csrService.getPendingCsrs()
        return ResponseEntity.ok(pending.map { it.toDto() })
    }

    /**
     * Get a specific CSR by ID.
     */
    @GetMapping("/csr/{id}")
    @PreAuthorize("hasAnyRole('CA_OPERATOR', 'ADMIN')")
    fun getCsr(@PathVariable id: Long): ResponseEntity<CsrRequestDto> {
        val csr = csrService.getCsrById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(csr.toDto())
    }

    /**
     * Upload signed certificate for a CSR.
     * CA Operator signs the CSR offline and uploads the result.
     */
    @PostMapping("/csr/{id}/certificate")
    @PreAuthorize("hasAnyRole('CA_OPERATOR', 'ADMIN')")
    fun uploadCertificate(
        @PathVariable id: Long,
        @RequestBody request: UploadCertificateRequest,
        @RequestHeader("X-Operator-Id", required = false) operatorId: String?
    ): ResponseEntity<Map<String, String>> {
        val operator = operatorId ?: "system"
        csrService.uploadSignedCertificate(id, request.certificate, operator)
        return ResponseEntity.ok(mapOf(
            "message" to "Certificate uploaded successfully",
            "csrId" to id.toString(),
            "status" to "SIGNED"
        ))
    }

    /**
     * Reject a CSR.
     */
    @PostMapping("/csr/{id}/reject")
    @PreAuthorize("hasAnyRole('CA_OPERATOR', 'ADMIN')")
    fun rejectCsr(
        @PathVariable id: Long,
        @RequestBody request: RejectCsrRequest,
        @RequestHeader("X-Operator-Id", required = false) operatorId: String?
    ): ResponseEntity<Map<String, String>> {
        val operator = operatorId ?: "system"
        csrService.rejectCsr(id, request.reason, operator)
        return ResponseEntity.ok(mapOf(
            "message" to "CSR rejected",
            "csrId" to id.toString(),
            "status" to "REJECTED"
        ))
    }
}

// DTOs
data class CsrRequestDto(
    val id: Long,
    val userId: String,
    val subjectDn: String,
    val status: CsrStatus,
    val csrPem: String,
    val signedCertificate: String?,
    val rejectionReason: String?,
    val createdAt: String,
    val processedAt: String?,
    val processedBy: String?
)

data class UploadCertificateRequest(val certificate: String)
data class RejectCsrRequest(val reason: String)

fun CsrRequest.toDto() = CsrRequestDto(
    id = id!!,
    userId = userId,
    subjectDn = subjectDn,
    status = status,
    csrPem = csrPem,
    signedCertificate = signedCertificate,
    rejectionReason = rejectionReason,
    createdAt = createdAt.toString(),
    processedAt = processedAt?.toString(),
    processedBy = processedBy
)
