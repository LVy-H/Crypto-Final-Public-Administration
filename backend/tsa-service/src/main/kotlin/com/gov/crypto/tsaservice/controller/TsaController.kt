package com.gov.crypto.tsaservice.controller

import com.gov.crypto.tsaservice.service.TsaService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * RFC 3161 Time-Stamp Protocol Controller.
 * 
 * Implements the HTTP binding for TSP as per RFC 3161 Section 3.4:
 * - Content-Type: application/timestamp-query (request)
 * - Content-Type: application/timestamp-reply (response)
 * 
 * Also supports application/octet-stream for broader client compatibility.
 */
@RestController
@RequestMapping("/tsa")
class TsaController(private val tsaService: TsaService) {

    companion object {
        const val TIMESTAMP_QUERY_MIME = "application/timestamp-query"
        const val TIMESTAMP_REPLY_MIME = "application/timestamp-reply"
    }

    /**
     * RFC 3161 Timestamp Request endpoint.
     * 
     * Accepts: application/timestamp-query or application/octet-stream
     * Returns: application/timestamp-reply (TimeStampResp ASN.1 DER encoded)
     */
    @PostMapping(
        "/stamp",
        consumes = [TIMESTAMP_QUERY_MIME, MediaType.APPLICATION_OCTET_STREAM_VALUE],
        produces = [TIMESTAMP_REPLY_MIME]
    )
    fun stamp(@RequestBody requestBytes: ByteArray): ResponseEntity<ByteArray> {
        val responseBytes = tsaService.generateTimeStampResponse(requestBytes)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(TIMESTAMP_REPLY_MIME))
            .body(responseBytes)
    }

    /**
     * TSA Information endpoint.
     * Returns basic information about this TSA for discovery.
     */
    @GetMapping("/info")
    fun info(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "name" to "Government PQC TSA",
            "version" to "1.0",
            "protocol" to "RFC 3161",
            "signatureAlgorithm" to "ML-DSA-65 (FIPS 204)",
            "acceptedDigestAlgorithms" to listOf(
                "SHA-256",
                "SHA-384", 
                "SHA-512",
                "SHA-512/256",
                "SHA3-256",
                "SHA3-384",
                "SHA3-512"
            ),
            "stampEndpoint" to "/api/v1/tsa/stamp",
            "contentType" to mapOf(
                "request" to TIMESTAMP_QUERY_MIME,
                "response" to TIMESTAMP_REPLY_MIME
            )
        ))
    }
}
