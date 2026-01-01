package com.gov.crypto.common.pqc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal test to verify BC 1.83 ML-DSA key generation and certificate signing.
 */
public class MlDsaMinimalTest {

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void shouldGenerateMlDsaKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ML-DSA", "BC");
        keyGen.initialize(MLDSAParameterSpec.ml_dsa_87, new SecureRandom());

        KeyPair keyPair = keyGen.generateKeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());

        System.out.println("Private key algorithm: " + keyPair.getPrivate().getAlgorithm());
        System.out.println("Private key format: " + keyPair.getPrivate().getFormat());
        System.out.println("Private key class: " + keyPair.getPrivate().getClass().getName());
    }

    @Test
    void shouldSignCertificateWithMlDsa() throws Exception {
        // Generate key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ML-DSA", "BC");
        keyGen.initialize(MLDSAParameterSpec.ml_dsa_87, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        // Build certificate
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);
        X500Name subject = new X500Name("CN=Test CA");
        BigInteger serial = new BigInteger(128, new SecureRandom());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        // Try signing - this is where the error might occur
        // Use the key's algorithm name obtained from the key itself
        String sigAlg = keyPair.getPrivate().getAlgorithm();
        System.out.println("Using signature algorithm: " + sigAlg);

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        assertNotNull(signer);

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));

        assertNotNull(cert);
        System.out.println("Certificate generated successfully!");
        System.out.println("Certificate subject: " + cert.getSubjectX500Principal());
    }
}
