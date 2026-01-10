package com.gov.crypto.pkiservice.service

import com.gov.crypto.pkiservice.entity.CsrRequest
import com.gov.crypto.pkiservice.entity.CsrStatus
import com.gov.crypto.pkiservice.repository.CsrRequestRepository
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.stereotype.Service
import java.security.Security
import java.time.Instant
import java.util.Base64

/**
 * CSR Service for Offline CA workflow.
 * 
 * This service queues CSRs for offline signing - the CA private key
 * is NEVER on the server. CA Operators download pending CSRs,
 * sign them offline, and upload the signed certificates.
 */
@Service
class CsrService(
    private val csrRequestRepository: CsrRequestRepository
) {

    init {
        // Ensure BC providers are registered for CSR verification
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastlePQCProvider())
        }
    }

    /**
     * Submit a CSR for certificate enrollment.
     * The CSR is queued for offline signing by a CA Operator.
     * 
     * @param csrBase64 Base64 encoded PKCS#10 CSR
     * @param userId The authenticated user's ID
     * @return The CSR request ID for tracking
     */
    fun submitCsr(csrBase64: String, userId: String): Long {
        // 1. Parse and validate CSR
        val csrBytes = Base64.getDecoder().decode(csrBase64)
        val csr = PKCS10CertificationRequest(csrBytes)

        // 2. Verify POP (Proof of Possession) - signature on CSR
        val verifierProvider = try {
            JcaContentVerifierProviderBuilder()
                .setProvider(BouncyCastlePQCProvider.PROVIDER_NAME)
                .build(csr.subjectPublicKeyInfo)
        } catch (e: Exception) {
            // Fallback to BC provider for classical algorithms
            JcaContentVerifierProviderBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(csr.subjectPublicKeyInfo)
        }

        if (!csr.isSignatureValid(verifierProvider)) {
            throw IllegalArgumentException("Invalid CSR Signature (POP Failed)")
        }

        // 3. Queue CSR for offline signing
        val csrRequest = CsrRequest(
            userId = userId,
            csrPem = csrBase64,
            subjectDn = csr.subject.toString(),
            status = CsrStatus.PENDING
        )
        
        val saved = csrRequestRepository.save(csrRequest)
        return saved.id!!
    }

    /**
     * Get pending CSRs for CA Operator.
     */
    fun getPendingCsrs(): List<CsrRequest> {
        return csrRequestRepository.findByStatus(CsrStatus.PENDING)
    }

    /**
     * Get CSR by ID.
     */
    fun getCsrById(id: Long): CsrRequest? {
        return csrRequestRepository.findById(id).orElse(null)
    }

    /**
     * Get user's CSR requests.
     */
    fun getUserCsrs(userId: String): List<CsrRequest> {
        return csrRequestRepository.findByUserId(userId)
    }

    /**
     * Upload signed certificate (CA Operator action).
     * 
     * @param csrId The CSR request ID
     * @param signedCertBase64 Base64 encoded X.509 certificate
     * @param operatorId The CA Operator's ID
     */
    fun uploadSignedCertificate(csrId: Long, signedCertBase64: String, operatorId: String) {
        val csrRequest = csrRequestRepository.findById(csrId)
            .orElseThrow { IllegalArgumentException("CSR not found: $csrId") }

        if (csrRequest.status != CsrStatus.PENDING) {
            throw IllegalStateException("CSR is not pending: ${csrRequest.status}")
        }

        csrRequest.signedCertificate = signedCertBase64
        csrRequest.status = CsrStatus.SIGNED
        csrRequest.processedAt = Instant.now()
        csrRequest.processedBy = operatorId

        csrRequestRepository.save(csrRequest)
    }

    /**
     * Reject a CSR (CA Operator action).
     */
    fun rejectCsr(csrId: Long, reason: String, operatorId: String) {
        val csrRequest = csrRequestRepository.findById(csrId)
            .orElseThrow { IllegalArgumentException("CSR not found: $csrId") }

        if (csrRequest.status != CsrStatus.PENDING) {
            throw IllegalStateException("CSR is not pending: ${csrRequest.status}")
        }

        csrRequest.status = CsrStatus.REJECTED
        csrRequest.rejectionReason = reason
        csrRequest.processedAt = Instant.now()
        csrRequest.processedBy = operatorId

        csrRequestRepository.save(csrRequest)
    }
}
