package com.gov.crypto.pkiservice.controller

import com.gov.crypto.pkiservice.service.CsrService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Enrollment Controller for certificate requests.
 * 
 * Implements Offline CA workflow:
 * - Users submit CSRs (queued as PENDING)
 * - CA Operators sign offline
 * - Users retrieve signed certificates
 */
@RestController
@RequestMapping("/pki")
class EnrollmentController(private val csrService: CsrService) {

    /**
     * Submit a CSR for certificate enrollment.
     * The CSR will be queued for offline signing by a CA Operator.
     * 
     * @return CSR request ID for tracking
     */
    @PostMapping("/enroll")
    fun enroll(
        @RequestBody request: EnrollmentRequest,
        @RequestHeader("X-User-Id", required = false) userId: String?
    ): ResponseEntity<EnrollmentResponse> {
        val user = userId ?: "anonymous"
        val csrId = csrService.submitCsr(request.csr, user)
        return ResponseEntity.ok(EnrollmentResponse(
            csrId = csrId,
            status = "PENDING",
            message = "CSR submitted successfully. Awaiting CA Operator approval."
        ))
    }

    /**
     * Get status of a CSR request.
     */
    @GetMapping("/csr/{id}/status")
    fun getCsrStatus(@PathVariable id: Long): ResponseEntity<CsrStatusResponse> {
        val csr = csrService.getCsrById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(CsrStatusResponse(
            csrId = csr.id!!,
            status = csr.status.name,
            signedCertificate = csr.signedCertificate,
            rejectionReason = csr.rejectionReason
        ))
    }

    /**
     * Get user's CSR requests.
     */
    @GetMapping("/my-requests")
    fun getMyRequests(
        @RequestHeader("X-User-Id", required = false) userId: String?
    ): ResponseEntity<List<CsrStatusResponse>> {
        val user = userId ?: return ResponseEntity.badRequest().build()
        val requests = csrService.getUserCsrs(user)
        return ResponseEntity.ok(requests.map { csr ->
            CsrStatusResponse(
                csrId = csr.id!!,
                status = csr.status.name,
                signedCertificate = csr.signedCertificate,
                rejectionReason = csr.rejectionReason
            )
        })
    }

    /**
     * Download signed certificate.
     */
    @GetMapping("/certificate/{csrId}")
    fun getCertificate(@PathVariable csrId: Long): ResponseEntity<CertificateResponse> {
        val csr = csrService.getCsrById(csrId)
            ?: return ResponseEntity.notFound().build()
        
        if (csr.signedCertificate == null) {
            return ResponseEntity.status(202).body(CertificateResponse(
                status = csr.status.name,
                certificate = null,
                message = "Certificate not yet available"
            ))
        }
        
        return ResponseEntity.ok(CertificateResponse(
            status = "SIGNED",
            certificate = csr.signedCertificate,
            message = "Certificate ready"
        ))
    }
}

// DTOs
data class EnrollmentRequest(val csr: String)
data class EnrollmentResponse(
    val csrId: Long,
    val status: String,
    val message: String
)
data class CsrStatusResponse(
    val csrId: Long,
    val status: String,
    val signedCertificate: String?,
    val rejectionReason: String?
)
data class CertificateResponse(
    val status: String,
    val certificate: String?,
    val message: String
)
