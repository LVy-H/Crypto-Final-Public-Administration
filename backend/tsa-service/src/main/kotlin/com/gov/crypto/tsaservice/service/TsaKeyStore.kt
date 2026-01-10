package com.gov.crypto.tsaservice.service

import com.gov.crypto.common.service.PqcAlgorithm
import com.gov.crypto.common.service.PqcCryptoService
import com.gov.crypto.common.util.KeyConverter
import jakarta.annotation.PostConstruct
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.springframework.stereotype.Service
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.util.Date

@Service
class TsaKeyStore(private val pqcCryptoService: PqcCryptoService) {

    init {
        // Ensure both providers are registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastlePQCProvider())
        }
    }

    private lateinit var tsaKeyPair: java.security.KeyPair
    private lateinit var tsaCertificate: java.security.cert.X509Certificate
    val tsaPolicyOID = "1.2.3.4.5.6.7.8" // Placeholder OID

    @PostConstruct
    fun init() {
        // TSA keys should be strictly ML-DSA for fast signing
        val bcKeyPair: AsymmetricCipherKeyPair = pqcCryptoService.generateKeyPair(PqcAlgorithm.ML_DSA_65)
        tsaKeyPair = KeyConverter.toJavaKeyPair(bcKeyPair)
        
        // Generate Self-Signed Cert using JCA (ML-DSA works with BC provider)
        val signer = org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("ML-DSA-65")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)  // Use BC not BCPQC
            .build(tsaKeyPair.private)
            
        val builder = org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
            org.bouncycastle.asn1.x500.X500Name("CN=Test TSA"),
            java.math.BigInteger.valueOf(System.currentTimeMillis()),
            Date(),
            Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000),
            org.bouncycastle.asn1.x500.X500Name("CN=Test TSA"),
            tsaKeyPair.public
        )
        
        val holder = builder.build(signer)
        tsaCertificate = org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)  // Use BC not BCPQC
            .getCertificate(holder)

        println("TSA KeyStore Initialized with Certificate.")
    }

    fun getPrivateKey(): PrivateKey = tsaKeyPair.private
    fun getPublicKey(): PublicKey = tsaKeyPair.public
    fun getCertificate(): java.security.cert.X509Certificate = tsaCertificate
    
    /**
     * Returns a certificate store containing the TSA certificate.
     * This is used to include the certificate in TimeStampResponse per RFC 3161.
     */
    fun getCertificateStore(): org.bouncycastle.util.Store<org.bouncycastle.cert.X509CertificateHolder> {
        val holder = org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(tsaCertificate)
        return org.bouncycastle.util.CollectionStore(listOf(holder))
    }
}
