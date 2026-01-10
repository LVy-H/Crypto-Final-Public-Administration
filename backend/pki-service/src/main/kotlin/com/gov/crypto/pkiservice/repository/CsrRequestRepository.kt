package com.gov.crypto.pkiservice.repository

import com.gov.crypto.pkiservice.entity.CsrRequest
import com.gov.crypto.pkiservice.entity.CsrStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CsrRequestRepository : JpaRepository<CsrRequest, Long> {
    fun findByUserId(userId: String): List<CsrRequest>
    fun findByStatus(status: CsrStatus): List<CsrRequest>
    fun findByUserIdAndStatus(userId: String, status: CsrStatus): List<CsrRequest>
}
