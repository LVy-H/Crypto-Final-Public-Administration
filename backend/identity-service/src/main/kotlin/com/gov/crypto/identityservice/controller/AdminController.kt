package com.gov.crypto.identityservice.controller

import com.gov.crypto.identityservice.entity.KycStatus
import com.gov.crypto.identityservice.entity.Roles
import com.gov.crypto.identityservice.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
class AdminController(private val userRepository: UserRepository) {

    data class KycApprovalRequest(val username: String, val action: String) // action: APPROVE or REJECT

    @PostMapping("/approve-kyc")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    fun approveKyc(@RequestBody request: KycApprovalRequest): ResponseEntity<String> {
        // Role check enforced by @PreAuthorize annotation
        
        val user = userRepository.findByUsername(request.username)
            .orElseThrow { RuntimeException("User not found") }

        if (request.action.uppercase() == "APPROVE") {
            user.kycStatus = KycStatus.APPROVED
        } else if (request.action.uppercase() == "REJECT") {
            user.kycStatus = KycStatus.REJECTED
        } else {
            return ResponseEntity.badRequest().body("Invalid action. Use APPROVE or REJECT")
        }
        
        userRepository.save(user)
        return ResponseEntity.ok("KYC status updated to ${user.kycStatus} for user ${request.username}")
    }
}
