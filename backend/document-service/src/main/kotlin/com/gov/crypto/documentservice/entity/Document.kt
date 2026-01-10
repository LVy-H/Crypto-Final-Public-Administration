package com.gov.crypto.documentservice.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Column
import java.time.Instant

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val ownerId: String, // from Identity Service

    @Column(nullable = false)
    val filename: String,

    @Column(nullable = false)
    val minioBucket: String,

    @Column(nullable = false)
    val minioObjectKey: String,

    @Column(nullable = false)
    val encryptedDek: String, // Base64 Encapsulated Key (ML-KEM)

    @Column(nullable = false)
    val uploadedAt: Instant = Instant.now()
)
