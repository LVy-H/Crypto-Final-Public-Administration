package com.gov.crypto.tsaservice.service

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.cmp.PKIFailureInfo
import org.bouncycastle.asn1.cmp.PKIStatus
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.tsp.TimeStampRequest
import org.bouncycastle.tsp.TimeStampResponse
import org.bouncycastle.tsp.TimeStampResponseGenerator
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.tsp.TimeStampTokenGenerator
import org.bouncycastle.tsp.TSPException
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.Date

/**
 * RFC 3161 Compliant Time-Stamp Authority Service.
 * 
 * This implementation follows the Time-Stamp Protocol (TSP) as defined in RFC 3161.
 * Uses ML-DSA-65 (FIPS 204) post-quantum signature algorithm for future-proof security.
 * 
 * Key RFC 3161 compliance points:
 * - Accepts TimeStampReq (ASN.1 DER encoded)
 * - Returns TimeStampResp with proper PKIStatusInfo
 * - Includes accurate genTime (generation time)
 * - Assigns unique serialNumber to each token
 * - Signs with TSA's private key and includes TSA certificate
 */
@Service
class TsaService(private val tsaKeyStore: TsaKeyStore) {

    // Accepted digest algorithms for timestamp requests (per RFC 3161)
    private val acceptedAlgorithms = setOf(
        NISTObjectIdentifiers.id_sha256,
        NISTObjectIdentifiers.id_sha384,
        NISTObjectIdentifiers.id_sha512,
        NISTObjectIdentifiers.id_sha512_256,
        NISTObjectIdentifiers.id_sha3_256,
        NISTObjectIdentifiers.id_sha3_384,
        NISTObjectIdentifiers.id_sha3_512
    )

    /**
     * Generates an RFC 3161 compliant TimeStampResponse.
     * 
     * @param requestBytes Raw ASN.1 DER encoded TimeStampReq
     * @return ASN.1 DER encoded TimeStampResp (includes status + optional token)
     */
    fun generateTimeStampResponse(requestBytes: ByteArray): ByteArray {
        return try {
            val request = TimeStampRequest(requestBytes)
            
            // Validate request per RFC 3161
            validateRequest(request)
            
            // Generate the token
            val tokenGenerator = createTokenGenerator()
            val responseGenerator = TimeStampResponseGenerator(tokenGenerator, acceptedAlgorithms)
            
            // Generate unique serial number (per RFC 3161, must be unique per TSA)
            val serialNumber = generateSerialNumber()
            
            // Create response with current time and serial
            val response = responseGenerator.generate(request, serialNumber, Date())
            
            response.encoded
        } catch (e: TSPException) {
            // Return error response per RFC 3161
            createErrorResponse(PKIFailureInfo.badRequest, e.message ?: "Invalid timestamp request")
        } catch (e: IllegalArgumentException) {
            createErrorResponse(PKIFailureInfo.badAlg, e.message ?: "Unsupported algorithm")
        } catch (e: Exception) {
            createErrorResponse(PKIFailureInfo.systemFailure, "Internal TSA error: ${e.message}")
        }
    }

    /**
     * Validates the TimeStampRequest per RFC 3161 requirements.
     */
    private fun validateRequest(request: TimeStampRequest) {
        // Check if the message imprint algorithm is supported
        val algOID = request.messageImprintAlgOID
        if (!acceptedAlgorithms.contains(algOID)) {
            throw IllegalArgumentException("Unsupported hash algorithm: $algOID")
        }
        
        // Validate message imprint (hash value) is present and has proper length
        val hashValue = request.messageImprintDigest
        if (hashValue == null || hashValue.isEmpty()) {
            throw TSPException("Message imprint digest is missing")
        }
        
        // Validate hash length matches algorithm
        val expectedLength = getExpectedHashLength(algOID)
        if (hashValue.size != expectedLength) {
            throw TSPException("Invalid hash length for algorithm $algOID: expected $expectedLength, got ${hashValue.size}")
        }
    }

    /**
     * Creates the TimeStampTokenGenerator configured with ML-DSA signing.
     */
    private fun createTokenGenerator(): TimeStampTokenGenerator {
        // Build content signer with ML-DSA-65 (PQC algorithm)
        val signer = JcaContentSignerBuilder("ML-DSA-65")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(tsaKeyStore.getPrivateKey())

        // Build digest calculator provider
        val digestCalculatorProvider = JcaDigestCalculatorProviderBuilder()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build()

        // Create signer info generator with TSA certificate
        val signerInfoGen = org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder(digestCalculatorProvider)
            .build(signer, tsaKeyStore.getCertificate())

        // Use SHA-512/256 for the token digest (consistent with PQC security levels)
        val digestCalculator = digestCalculatorProvider.get(
            org.bouncycastle.asn1.x509.AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_256)
        )

        // Create generator with TSA policy OID
        val tokenGenerator = TimeStampTokenGenerator(
            signerInfoGen, 
            digestCalculator, 
            ASN1ObjectIdentifier(tsaKeyStore.tsaPolicyOID)
        )

        // Include TSA certificate in the response for verification
        tokenGenerator.addCertificates(tsaKeyStore.getCertificateStore())

        return tokenGenerator
    }

    /**
     * Generates a unique serial number for the timestamp token.
     * Per RFC 3161, serial numbers must be unique within the TSA.
     */
    private fun generateSerialNumber(): BigInteger {
        // Combine current time in nanos with random component for uniqueness
        val timeComponent = System.nanoTime()
        val randomComponent = (Math.random() * 1000000).toLong()
        return BigInteger.valueOf(timeComponent).add(BigInteger.valueOf(randomComponent))
    }

    /**
     * Creates an error TimeStampResponse per RFC 3161.
     */
    private fun createErrorResponse(failureInfo: Int, statusString: String): ByteArray {
        val responseGenerator = TimeStampResponseGenerator(null, acceptedAlgorithms)
        val status = when (failureInfo) {
            PKIFailureInfo.badAlg -> PKIStatus.REJECTION
            PKIFailureInfo.badRequest -> PKIStatus.REJECTION
            PKIFailureInfo.badDataFormat -> PKIStatus.REJECTION
            else -> PKIStatus.REJECTION
        }
        val response = responseGenerator.generateFailResponse(status, failureInfo, statusString)
        return response.encoded
    }

    /**
     * Returns expected hash length in bytes for the given algorithm OID.
     */
    private fun getExpectedHashLength(algOID: ASN1ObjectIdentifier): Int {
        return when (algOID) {
            NISTObjectIdentifiers.id_sha256 -> 32
            NISTObjectIdentifiers.id_sha384 -> 48
            NISTObjectIdentifiers.id_sha512 -> 64
            NISTObjectIdentifiers.id_sha512_256 -> 32
            NISTObjectIdentifiers.id_sha3_256 -> 32
            NISTObjectIdentifiers.id_sha3_384 -> 48
            NISTObjectIdentifiers.id_sha3_512 -> 64
            else -> 32 // Default
        }
    }
}
