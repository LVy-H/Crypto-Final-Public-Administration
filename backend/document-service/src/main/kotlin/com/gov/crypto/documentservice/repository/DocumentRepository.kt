package com.gov.crypto.documentservice.repository

import com.gov.crypto.documentservice.entity.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    fun findByOwnerId(ownerId: String): List<Document>
}
