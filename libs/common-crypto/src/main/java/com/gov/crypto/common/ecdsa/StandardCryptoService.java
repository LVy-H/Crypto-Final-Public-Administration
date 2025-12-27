package com.gov.crypto.common.ecdsa;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

/**
 * Standard Cryptography Service using ECDSA P-384.
 * 
 * Provides browser/PDF-reader compatible signatures that show "green checkmark"
 * in Adobe Reader, Foxit, and other standard viewers.
 * 
 * Used as the PRIMARY signature in hybrid signing mode.
 * Dilithium (PQC) is embedded as secondary for future-proofing.
 */
public class StandardCryptoService {

    private static final Logger log = LoggerFactory.getLogger(StandardCryptoService.class);
    private static final String ECDSA_ALGORITHM = "SHA384withECDSA";
    private static final String EC_CURVE = "secp384r1"; // P-384

    static {
        // Register Bouncy Castle provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generate ECDSA P-384 key pair.
     * This key is used for browser-compatible signing.
     */
    public KeyPair generateEcdsaKeyPair() throws Exception {
        log.info("Generating ECDSA P-384 key pair");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(new ECGenParameterSpec(EC_CURVE), new SecureRandom());

        KeyPair keyPair = keyGen.generateKeyPair();
        log.info("ECDSA P-384 key pair generated successfully");

        return keyPair;
    }

    /**
     * Sign data using ECDSA P-384 private key.
     * Returns DER-encoded signature.
     */
    public byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(ECDSA_ALGORITHM, "BC");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * Verify ECDSA signature.
     */
    public boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance(ECDSA_ALGORITHM, "BC");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }

    /**
     * Generate self-signed X.509 certificate for ECDSA key.
     * Used for development/testing. In production, use CSR workflow.
     */
    public X509Certificate generateSelfSignedCertificate(
            KeyPair keyPair,
            String subjectDn,
            int validDays) throws Exception {

        log.info("Generating self-signed ECDSA certificate for: {}", subjectDn);

        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (long) validDays * 24 * 60 * 60 * 1000);

        X500Name subject = new X500Name(subjectDn);
        BigInteger serial = new BigInteger(128, new SecureRandom());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                serial,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic());

        // Add basic constraints (end-entity certificate)
        certBuilder.addExtension(
                org.bouncycastle.asn1.x509.Extension.basicConstraints,
                true,
                new org.bouncycastle.asn1.x509.BasicConstraints(false));

        // Add key usage for digital signatures
        certBuilder.addExtension(
                org.bouncycastle.asn1.x509.Extension.keyUsage,
                true,
                new org.bouncycastle.asn1.x509.KeyUsage(
                        org.bouncycastle.asn1.x509.KeyUsage.digitalSignature |
                                org.bouncycastle.asn1.x509.KeyUsage.nonRepudiation));

        ContentSigner signer = new JcaContentSignerBuilder(ECDSA_ALGORITHM)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));
    }

    /**
     * Generate Certificate Signing Request (CSR).
     */
    public String generateCsr(KeyPair keyPair, String subjectDn) throws Exception {
        log.info("Generating CSR for: {}", subjectDn);

        X500Name subject = new X500Name(subjectDn);

        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(ECDSA_ALGORITHM)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        return csrToPem(csrBuilder.build(signer));
    }

    /**
     * Convert certificate to PEM format.
     */
    public String certificateToPem(X509Certificate cert) throws Exception {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            pemWriter.writeObject(cert);
        }
        return sw.toString();
    }

    /**
     * Convert public key to PEM format.
     */
    public String publicKeyToPem(PublicKey publicKey) throws Exception {
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        }
        return sw.toString();
    }

    /**
     * Convert private key to PEM format.
     * WARNING: Handle with extreme care. Never log or expose.
     */
    public String privateKeyToPem(PrivateKey privateKey) throws Exception {
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("EC PRIVATE KEY", privateKey.getEncoded()));
        }
        return sw.toString();
    }

    /**
     * Convert CSR to PEM format.
     */
    private String csrToPem(org.bouncycastle.pkcs.PKCS10CertificationRequest csr) throws Exception {
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded()));
        }
        return sw.toString();
    }

    /**
     * Get the signature algorithm name.
     */
    public String getSignatureAlgorithm() {
        return ECDSA_ALGORITHM;
    }

    /**
     * Get the curve name.
     */
    public String getCurveName() {
        return EC_CURVE;
    }
}
