package com.gov.crypto.common.service

import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyGenerationParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyPairGenerator
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAKeyGenerationParameters
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAKeyPairGenerator
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAParameters
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAPrivateKeyParameters
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAPublicKeyParameters
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSASigner
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.pqc.crypto.MessageSigner
import org.bouncycastle.crypto.Signer
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.bc.BcContentSignerBuilder
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.io.StringWriter
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.util.Date
import org.springframework.stereotype.Service
import java.security.SecureRandom

enum class PqcAlgorithm(val oid: String) {
    ML_DSA_44("2.16.840.1.101.3.4.3.17"), // Dilithium2
    ML_DSA_65("2.16.840.1.101.3.4.3.18"), // Dilithium3
    ML_DSA_87("2.16.840.1.101.3.4.3.19"), // Dilithium5
    SLH_DSA_SHAKE_128F("2.16.840.1.101.3.4.3.24"), // Sphincs+
    ML_KEM_768("2.16.840.1.101.3.4.3.2")  // Kyber-768
}

@Service
class PqcCryptoService {

    private val random = SecureRandom()

    // --- Key Generation ---

    fun generateKeyPair(algorithm: PqcAlgorithm): AsymmetricCipherKeyPair {
        return when (algorithm) {
            PqcAlgorithm.ML_DSA_44 -> {
                val generator = MLDSAKeyPairGenerator()
                generator.init(MLDSAKeyGenerationParameters(random, MLDSAParameters.ml_dsa_44))
                generator.generateKeyPair()
            }
            PqcAlgorithm.ML_DSA_65 -> {
                val generator = MLDSAKeyPairGenerator()
                generator.init(MLDSAKeyGenerationParameters(random, MLDSAParameters.ml_dsa_65))
                generator.generateKeyPair()
            }
            PqcAlgorithm.ML_DSA_87 -> {
                val generator = MLDSAKeyPairGenerator()
                generator.init(MLDSAKeyGenerationParameters(random, MLDSAParameters.ml_dsa_87))
                generator.generateKeyPair()
            }
            PqcAlgorithm.SLH_DSA_SHAKE_128F -> {
                val generator = SLHDSAKeyPairGenerator()
                generator.init(SLHDSAKeyGenerationParameters(random, SLHDSAParameters.sha2_128f))
                generator.generateKeyPair()
            }
            PqcAlgorithm.ML_KEM_768 -> {
                 val generator = org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator()
                 generator.init(org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters(random, org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters.ml_kem_768))
                 generator.generateKeyPair()
            }
        }
    }

    // --- Signing ---

    fun sign(privateKey: AsymmetricKeyParameter, message: ByteArray): ByteArray {
        return when (privateKey) {
            is MLDSAPrivateKeyParameters -> {
                val signer: Signer = MLDSASigner()
                signer.init(true, privateKey)
                signer.update(message, 0, message.size)
                signer.generateSignature()
            }
            is SLHDSAPrivateKeyParameters -> {
                val signer: MessageSigner = SLHDSASigner()
                signer.init(true, privateKey)
                signer.generateSignature(message)
            }
            else -> throw IllegalArgumentException("Unsupported key type: ${privateKey.javaClass.name}")
        }
    }

    // --- Verification ---

    fun verify(publicKey: AsymmetricKeyParameter, message: ByteArray, signature: ByteArray): Boolean {
        return when (publicKey) {
            is MLDSAPublicKeyParameters -> {
                val signer: Signer = MLDSASigner()
                signer.init(false, publicKey)
                signer.update(message, 0, message.size)
                signer.verifySignature(signature)
            }
            is SLHDSAPublicKeyParameters -> {
                val signer: MessageSigner = SLHDSASigner()
                signer.init(false, publicKey)
                signer.verifySignature(message, signature)
            }
            else -> throw IllegalArgumentException("Unsupported key type: ${publicKey.javaClass.name}")
        }
    }

    // --- KEM (Key Encapsulation) ---

    fun encapsulate(publicKey: AsymmetricKeyParameter): org.bouncycastle.crypto.SecretWithEncapsulation {
        val kemGenerator = org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator(random)
        return kemGenerator.generateEncapsulated(publicKey)
    }

    fun decapsulate(privateKey: AsymmetricKeyParameter, encapsulation: ByteArray): ByteArray {
        val kemExtractor = org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor(privateKey as org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters)
        return kemExtractor.extractSecret(encapsulation)
    }


    // --- Certificate Management ---

    fun generateSelfSignedCertificate(
        keyPair: AsymmetricCipherKeyPair,
        subjectDn: String,
        daysResult: Int,
        algorithm: PqcAlgorithm
    ): X509Certificate {
        val publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keyPair.public)
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + daysResult * 24L * 60 * 60 * 1000)
        val serialNumber = BigInteger(64, random)
        
        val signer = buildContentSigner(keyPair.private, algorithm)
        
        val builder = X509v3CertificateBuilder(
            X500Name(subjectDn),
            serialNumber,
            notBefore,
            notAfter,
            X500Name(subjectDn),
            publicKeyInfo
        )

        // Extensions
        val extUtils = JcaX509ExtensionUtils()
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))

        val certHolder = builder.build(signer)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)
    }

    fun generateCsr(
        keyPair: AsymmetricCipherKeyPair,
        subjectDn: String,
        algorithm: PqcAlgorithm
    ): PKCS10CertificationRequest {
        val publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keyPair.public)
        val signer = buildContentSigner(keyPair.private, algorithm)
        
        val builder = org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder(
            X500Name(subjectDn),
            publicKeyInfo
        )
        
        return builder.build(signer)
    }
    
    fun csrToPem(csr: PKCS10CertificationRequest): String {
        val sw = StringWriter()
        val pw = PemWriter(sw)
        pw.writeObject(PemObject("CERTIFICATE REQUEST", csr.encoded))
        pw.close()
        return sw.toString()
    }
    
    fun signCsr(
        csr: PKCS10CertificationRequest,
        issuerKey: AsymmetricKeyParameter,
        issuerCert: X509Certificate,
        days: Int,
        algorithm: PqcAlgorithm,
        isCa: Boolean
    ): X509Certificate {
        val publicKeyInfo = csr.subjectPublicKeyInfo
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + days * 24L * 60 * 60 * 1000)
        val serialNumber = BigInteger(64, random)

        val signer = buildContentSigner(issuerKey, algorithm)

        val builder = X509v3CertificateBuilder(
            X500Name(issuerCert.subjectX500Principal.name),
            serialNumber,
            notBefore,
            notAfter,
            csr.subject,
            publicKeyInfo
        )

        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(isCa))
        if(isCa) {
             builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
        } else {
             builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment))
        }

        val certHolder = builder.build(signer)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)
    }

    private fun buildContentSigner(privateKey: AsymmetricKeyParameter, algorithm: PqcAlgorithm): ContentSigner {
        // Map PqcAlgorithm to BC AlgorithmIdentifier/Signer
        // Note: For BC PQC, we can use specific signer builders or generic one if OID matches.
        // Usually, we use org.bouncycastle.pqc.crypto.*Signer and wrap in ContentSigner.
        // But X509v3CertificateBuilder expects ContentSigner.
        // We can use BcContentSignerBuilder? No, PQC support is specific.
        
        // Easier: Use JcaContentSignerBuilder if we had Java Keys. But we have BC params.
        // So we must use Bc impls.
        
        // For ML-DSA:
        val sigAlgId = when(algorithm) {
             PqcAlgorithm.ML_DSA_44 -> org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_ml_dsa_44
             PqcAlgorithm.ML_DSA_65 -> org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_ml_dsa_65
             PqcAlgorithm.ML_DSA_87 -> org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_ml_dsa_87
             PqcAlgorithm.SLH_DSA_SHAKE_128F -> org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_slh_dsa_shake_128f
             else -> throw IllegalArgumentException("Unsupported sig algo: $algorithm")
        }
        
         return object : ContentSigner {
            private val stream = java.io.ByteArrayOutputStream()
            
            override fun getAlgorithmIdentifier(): org.bouncycastle.asn1.x509.AlgorithmIdentifier {
                return org.bouncycastle.asn1.x509.AlgorithmIdentifier(sigAlgId)
            }

            override fun getOutputStream(): java.io.OutputStream {
                return stream
            }

            override fun getSignature(): ByteArray {
                return sign(privateKey, stream.toByteArray())
            }
        }
    }

    // --- PEM Utils ---

    fun privateKeyToPem(key: AsymmetricKeyParameter): String {
        val sw = StringWriter()
        val info = org.bouncycastle.pqc.crypto.util.PrivateKeyInfoFactory.createPrivateKeyInfo(key)
        val pw = PemWriter(sw)
        pw.writeObject(PemObject("PRIVATE KEY", info.encoded))
        pw.close()
        return sw.toString()
    }
    
    fun publicKeyToPem(key: AsymmetricKeyParameter): String {
        val sw = StringWriter()
        val info = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key)
         val pw = PemWriter(sw)
        pw.writeObject(PemObject("PUBLIC KEY", info.encoded))
        pw.close()
        return sw.toString()
    }

    fun certificateToPem(cert: X509Certificate): String {
        val sw = StringWriter()
        val pw = PemWriter(sw)
        pw.writeObject(PemObject("CERTIFICATE", cert.encoded))
        pw.close()
        return sw.toString()
    }
    
    fun parsePrivateKeyPem(pem: String): AsymmetricKeyParameter {
        // Removing headers/footers for simple Base64 decode OR use PemReader?
        // Using PrivateKeyFactory.createKey(byte[])
        // We need to decode the PEM content.
        // Simplest: use BouncyCastle's PEMParser but we need 'bcpkix' dep which we have.
        // Or manual strip.
        // Let's use manual strip for simplicity if we don't want to import PEMParser.
        val content = pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = java.util.Base64.getDecoder().decode(content)
        return PrivateKeyFactory.createKey(bytes)
    }

    fun parseCertificatePem(pem: String): X509Certificate {
         val cf = java.security.cert.CertificateFactory.getInstance("X.509", "BC")
         return cf.generateCertificate(java.io.ByteArrayInputStream(pem.toByteArray())) as X509Certificate
    }

    fun parseCsrPem(pem: String): PKCS10CertificationRequest {
        val content = pem.replace("-----BEGIN CERTIFICATE REQUEST-----", "")
            .replace("-----END CERTIFICATE REQUEST-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = java.util.Base64.getDecoder().decode(content)
        return PKCS10CertificationRequest(bytes)
    }
    
    fun getSubjectDnFromCsr(csr: PKCS10CertificationRequest): String {
        return csr.subject.toString()
    }
    
    fun getPublicKeyFromCsr(csr: PKCS10CertificationRequest): AsymmetricKeyParameter {
        return org.bouncycastle.pqc.crypto.util.PublicKeyFactory.createKey(csr.subjectPublicKeyInfo)
    }
}
