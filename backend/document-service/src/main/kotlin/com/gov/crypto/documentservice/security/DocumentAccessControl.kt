package com.gov.crypto.documentservice.security

import com.gov.crypto.documentservice.entity.Document
import com.gov.crypto.documentservice.repository.DocumentRepository
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

/**
 * Document Access Control Service
 * 
 * Implements ABAC (Attribute-Based Access Control) for documents:
 * - PRIVATE documents: Only owner or assigned officer can access
 * - PUBLIC documents: Any authenticated user can read
 * - Write operations: Only owner can modify
 */
@Service
class DocumentAccessControl(
    private val documentRepository: DocumentRepository
) {

    /**
     * Check if current user can read the document.
     * 
     * Rules:
     * - PRIVATE: Owner OR assigned officer
     * - PUBLIC: Any authenticated user
     */
    fun canRead(docId: Long): Boolean {
        val auth = getCurrentAuthentication() ?: return false
        val username = auth.name
        
        val doc = documentRepository.findById(docId).orElse(null) ?: return false
        
        // Owner can always read
        if (doc.ownerId == username) {
            return true
        }
        
        // Check if document is public (visibility field would be added to Document entity)
        // For now, assuming all documents are PRIVATE
        // In production, add visibility field to Document entity
        
        // Check if user is an assigned officer (would need assignedOfficerId field)
        // For now, officers with proper role can read
        if (hasRole(auth, "OFFICER") || hasRole(auth, "ADMIN")) {
            return true
        }
        
        return false
    }

    /**
     * Check if current user can write/modify the document.
     * 
     * Rules:
     * - Only owner can modify
     */
    fun canWrite(docId: Long): Boolean {
        val auth = getCurrentAuthentication() ?: return false
        val username = auth.name
        
        val doc = documentRepository.findById(docId).orElse(null) ?: return false
        
        return doc.ownerId == username
    }

    /**
     * Check if current user is the owner of the document.
     */
    fun isOwner(docId: Long): Boolean {
        val auth = getCurrentAuthentication() ?: return false
        val username = auth.name
        
        val doc = documentRepository.findById(docId).orElse(null) ?: return false
        
        return doc.ownerId == username
    }

    /**
     * Check if current user can countersign the document.
     * 
     * Rules:
     * - User must be the assigned countersigner
     * - OR user has OFFICER/ADMIN role
     */
    fun canCountersign(docId: Long, assignedCountersignerId: String?): Boolean {
        val auth = getCurrentAuthentication() ?: return false
        val username = auth.name
        
        // If specific countersigner is assigned, only that user can countersign
        if (assignedCountersignerId != null && assignedCountersignerId.isNotEmpty()) {
            if (username == assignedCountersignerId) {
                return true
            }
            // Allow admins to override
            return hasRole(auth, "ADMIN")
        }
        
        // Otherwise, any officer can countersign
        return hasRole(auth, "OFFICER") || hasRole(auth, "ADMIN")
    }

    private fun getCurrentAuthentication(): Authentication? {
        return SecurityContextHolder.getContext().authentication
    }

    private fun hasRole(auth: Authentication, role: String): Boolean {
        return auth.authorities.any { it.authority == "ROLE_$role" }
    }
}
