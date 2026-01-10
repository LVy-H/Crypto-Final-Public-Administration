package com.gov.crypto.pkiservice.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * CA Controller for public CA certificate access.
 * 
 * The CA certificate is loaded from configuration (offline CA model).
 * The CA private key is NEVER on the server - it stays offline.
 */
@RestController
@RequestMapping("/pki/ca")
class CaController(
    @Value("\${pki.ca.certificate:}") private val caCertBase64: String,
    @Value("\${pki.ca.subject:CN=Offline CA}") private val caSubject: String,
    @Value("\${pki.ca.issuer:CN=Offline CA}") private val caIssuer: String
) {

    /**
     * Get CA certificate information.
     * Returns the public CA certificate for chain validation.
     */
    @GetMapping("/info")
    fun getCaInfo(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "subject" to caSubject,
            "issuer" to caIssuer,
            "certificate" to caCertBase64,
            "note" to "CA private key is offline. Certificates are signed by authorized CA Operators."
        ))
    }
}
