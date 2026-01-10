package com.gov.crypto.common.util

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.bouncycastle.pqc.crypto.util.PrivateKeyInfoFactory
import org.bouncycastle.pqc.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security

object KeyConverter {

    init {
        // Register BOTH providers - BC for base crypto, BCPQC for PQC algorithms
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastlePQCProvider())
        }
    }

    // Use BC provider for PQC key conversion (ML-DSA KeyFactory is in BC, not BCPQC)
    private val converter = JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)

    fun toJavaPrivateKey(params: AsymmetricKeyParameter): PrivateKey {
        val info = PrivateKeyInfoFactory.createPrivateKeyInfo(params)
        return converter.getPrivateKey(info)
    }

    fun toJavaPublicKey(params: AsymmetricKeyParameter): PublicKey {
        val info = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(params)
        return converter.getPublicKey(info)
    }

    fun toJavaKeyPair(pair: AsymmetricCipherKeyPair): java.security.KeyPair {
        return java.security.KeyPair(
            toJavaPublicKey(pair.public),
            toJavaPrivateKey(pair.private)
        )
    }
}
