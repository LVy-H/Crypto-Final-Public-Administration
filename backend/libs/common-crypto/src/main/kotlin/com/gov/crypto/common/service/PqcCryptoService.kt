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
import org.bouncycastle.pqc.crypto.MessageSigner
import org.bouncycastle.crypto.Signer
import org.springframework.stereotype.Service
import java.security.SecureRandom

enum class PqcAlgorithm {
    ML_DSA_65,
    SLH_DSA_SHAKE_128F,
    ML_KEM_768 // Kyber-768
}

@Service
class PqcCryptoService {

    private val random = SecureRandom()

    // --- Key Generation ---

    fun generateKeyPair(algorithm: PqcAlgorithm): AsymmetricCipherKeyPair {
        return when (algorithm) {
            PqcAlgorithm.ML_DSA_65 -> {
                val generator = MLDSAKeyPairGenerator()
                generator.init(MLDSAKeyGenerationParameters(random, MLDSAParameters.ml_dsa_65))
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
}
