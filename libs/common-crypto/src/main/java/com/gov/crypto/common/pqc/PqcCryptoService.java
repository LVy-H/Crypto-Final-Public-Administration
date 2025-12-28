package com.gov.crypto.common.pqc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.io.StringWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-Quantum Cryptography (PQC) Service using Bouncy Castle.
 * Supports ML-DSA (Dilithium) algorithms for CA operations.
 */
public class PqcCryptoService {

        private static final Logger log = LoggerFactory.getLogger(PqcCryptoService.class);

        static {
                // Register Bouncy Castle providers
                Security.addProvider(new BouncyCastleProvider());
                Security.addProvider(new BouncyCastlePQCProvider());
        }

        /**
         * Supported ML-DSA algorithm levels
         */
        public enum MlDsaLevel {
                ML_DSA_44("Dilithium2", DilithiumParameterSpec.dilithium2), // NIST Level 2
                ML_DSA_65("Dilithium3", DilithiumParameterSpec.dilithium3), // NIST Level 3
                ML_DSA_87("Dilithium5", DilithiumParameterSpec.dilithium5); // NIST Level 5

                private final String algorithmName;
                private final DilithiumParameterSpec spec;

                MlDsaLevel(String algorithmName, DilithiumParameterSpec spec) {
                        this.algorithmName = algorithmName;
                        this.spec = spec;
                }

                public String getAlgorithmName() {
                        return algorithmName;
                }

                public DilithiumParameterSpec getSpec() {
                        return spec;
                }
        }

        /**
         * Generate ML-DSA (Dilithium) key pair
         */
        public KeyPair generateMlDsaKeyPair(MlDsaLevel level) throws Exception {
                log.info("Generating ML-DSA key pair with level: {}", level);

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
                keyGen.initialize(level.getSpec(), new SecureRandom());

                KeyPair keyPair = keyGen.generateKeyPair();
                log.info("ML-DSA key pair generated successfully");

                return keyPair;
        }

        /**
         * Generate self-signed X.509 certificate (for Root CA)
         */
        public X509Certificate generateSelfSignedCertificate(
                        KeyPair keyPair,
                        String subjectDn,
                        int validDays,
                        MlDsaLevel level) throws Exception {

                log.info("Generating self-signed certificate for: {}", subjectDn);

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

                // Add CA extensions
                certBuilder.addExtension(
                                org.bouncycastle.asn1.x509.Extension.basicConstraints,
                                true,
                                new org.bouncycastle.asn1.x509.BasicConstraints(true));

                certBuilder.addExtension(
                                org.bouncycastle.asn1.x509.Extension.keyUsage,
                                true,
                                new org.bouncycastle.asn1.x509.KeyUsage(
                                                org.bouncycastle.asn1.x509.KeyUsage.keyCertSign |
                                                                org.bouncycastle.asn1.x509.KeyUsage.cRLSign));

                ContentSigner signer = new JcaContentSignerBuilder(level.getAlgorithmName())
                                .setProvider("BCPQC")
                                .build(keyPair.getPrivate());

                X509CertificateHolder certHolder = certBuilder.build(signer);

                X509Certificate cert = new JcaX509CertificateConverter()
                                .setProvider("BC")
                                .getCertificate(certHolder);

                log.info("Self-signed certificate generated successfully");
                return cert;
        }

        /**
         * Generate subordinate certificate signed by parent CA
         */
        public X509Certificate generateSubordinateCertificate(
                        KeyPair subordinateKeyPair,
                        String subordinateDn,
                        X509Certificate issuerCert,
                        PrivateKey issuerPrivateKey,
                        int validDays,
                        boolean isCA,
                        MlDsaLevel level) throws Exception {

                log.info("Generating subordinate certificate for: {}", subordinateDn);

                Date notBefore = new Date();
                Date notAfter = new Date(System.currentTimeMillis() + (long) validDays * 24 * 60 * 60 * 1000);

                X500Name issuer = new X500Name(issuerCert.getSubjectX500Principal().getName());
                X500Name subject = new X500Name(subordinateDn);
                BigInteger serial = new BigInteger(128, new SecureRandom());

                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                                issuer,
                                serial,
                                notBefore,
                                notAfter,
                                subject,
                                subordinateKeyPair.getPublic());

                // Add extensions based on whether this is a CA or end-entity
                if (isCA) {
                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.basicConstraints,
                                        true,
                                        new org.bouncycastle.asn1.x509.BasicConstraints(0)); // pathlen:0

                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.keyUsage,
                                        true,
                                        new org.bouncycastle.asn1.x509.KeyUsage(
                                                        org.bouncycastle.asn1.x509.KeyUsage.keyCertSign |
                                                                        org.bouncycastle.asn1.x509.KeyUsage.cRLSign));
                } else {
                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.basicConstraints,
                                        true,
                                        new org.bouncycastle.asn1.x509.BasicConstraints(false));

                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.keyUsage,
                                        true,
                                        new org.bouncycastle.asn1.x509.KeyUsage(
                                                        org.bouncycastle.asn1.x509.KeyUsage.digitalSignature |
                                                                        org.bouncycastle.asn1.x509.KeyUsage.nonRepudiation));
                }

                ContentSigner signer = new JcaContentSignerBuilder(level.getAlgorithmName())
                                .setProvider("BCPQC")
                                .build(issuerPrivateKey);

                X509CertificateHolder certHolder = certBuilder.build(signer);

                X509Certificate cert = new JcaX509CertificateConverter()
                                .setProvider("BC")
                                .getCertificate(certHolder);

                log.info("Subordinate certificate generated successfully");
                return cert;
        }

        /**
         * Generate subordinate certificate from PEM-encoded issuer materials
         */
        public X509Certificate generateSubordinateCertificate(
                        PublicKey subordinatePublicKey,
                        String subordinateDn,
                        String issuerPrivateKeyPem,
                        String issuerCertPem,
                        int validDays,
                        MlDsaLevel signingLevel,
                        boolean isCA) throws Exception {

                log.info("Generating subordinate certificate (PEM) for: {}", subordinateDn);

                // Parse issuer certificate
                X509Certificate issuerCert = parseCertificatePem(issuerCertPem);

                // Parse issuer private key
                PrivateKey issuerPrivateKey = parsePrivateKeyPem(issuerPrivateKeyPem);

                Date notBefore = new Date();
                Date notAfter = new Date(System.currentTimeMillis() + (long) validDays * 24 * 60 * 60 * 1000);

                X500Name issuer = new X500Name(issuerCert.getSubjectX500Principal().getName());
                X500Name subject = new X500Name(subordinateDn);
                BigInteger serial = new BigInteger(128, new SecureRandom());

                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                                issuer,
                                serial,
                                notBefore,
                                notAfter,
                                subject,
                                subordinatePublicKey);

                // Add extensions
                if (isCA) {
                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.basicConstraints, true,
                                        new org.bouncycastle.asn1.x509.BasicConstraints(0));
                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.keyUsage, true,
                                        new org.bouncycastle.asn1.x509.KeyUsage(
                                                        org.bouncycastle.asn1.x509.KeyUsage.keyCertSign |
                                                                        org.bouncycastle.asn1.x509.KeyUsage.cRLSign));
                } else {
                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.basicConstraints, true,
                                        new org.bouncycastle.asn1.x509.BasicConstraints(false));
                        certBuilder.addExtension(
                                        org.bouncycastle.asn1.x509.Extension.keyUsage, true,
                                        new org.bouncycastle.asn1.x509.KeyUsage(
                                                        org.bouncycastle.asn1.x509.KeyUsage.digitalSignature |
                                                                        org.bouncycastle.asn1.x509.KeyUsage.nonRepudiation));
                }

                ContentSigner signer = new JcaContentSignerBuilder(signingLevel.getAlgorithmName())
                                .setProvider("BCPQC")
                                .build(issuerPrivateKey);

                X509CertificateHolder certHolder = certBuilder.build(signer);

                return new JcaX509CertificateConverter()
                                .setProvider("BC")
                                .getCertificate(certHolder);
        }

        /**
         * Parse PEM-encoded certificate
         */
        public X509Certificate parseCertificatePem(String pem) throws Exception {
                if (pem == null || pem.isBlank()) {
                        throw new IllegalArgumentException("PEM string is null or empty");
                }

                // Use standard CertificateFactory which is robust for PEM with headers
                java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(
                                pem.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                java.security.cert.CertificateFactory factory = java.security.cert.CertificateFactory
                                .getInstance("X.509", "BC");
                return (X509Certificate) factory.generateCertificate(stream);
        }

        /**
         * Parse PEM-encoded private key
         */
        public PrivateKey parsePrivateKeyPem(String pem) throws Exception {
                java.io.StringReader sr = new java.io.StringReader(pem);
                org.bouncycastle.openssl.PEMParser parser = new org.bouncycastle.openssl.PEMParser(sr);
                Object obj = parser.readObject();
                parser.close();

                if (obj instanceof org.bouncycastle.openssl.PEMKeyPair keyPair) {
                        return new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                                        .setProvider("BCPQC")
                                        .getKeyPair(keyPair)
                                        .getPrivate();
                } else if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo keyInfo) {
                        return new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                                        .setProvider("BCPQC")
                                        .getPrivateKey(keyInfo);
                }
                throw new IllegalArgumentException("Not a valid private key PEM");
        }

        /**
         * Sign data using ML-DSA private key
         */
        public byte[] sign(byte[] data, PrivateKey privateKey, MlDsaLevel level) throws Exception {
                Signature signature = Signature.getInstance(level.getAlgorithmName(), "BCPQC");
                signature.initSign(privateKey);
                signature.update(data);
                return signature.sign();
        }

        /**
         * Verify signature using ML-DSA public key
         */
        public boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey, MlDsaLevel level)
                        throws Exception {
                Signature signature = Signature.getInstance(level.getAlgorithmName(), "BCPQC");
                signature.initVerify(publicKey);
                signature.update(data);
                return signature.verify(signatureBytes);
        }

        /**
         * Convert certificate to PEM format
         */
        public String certificateToPem(X509Certificate cert) throws Exception {
                StringWriter sw = new StringWriter();
                try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
                        pemWriter.writeObject(cert);
                }
                return sw.toString();
        }

        /**
         * Convert public key to PEM format
         */
        public String publicKeyToPem(PublicKey publicKey) throws Exception {
                StringWriter sw = new StringWriter();
                try (PemWriter pemWriter = new PemWriter(sw)) {
                        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
                }
                return sw.toString();
        }

        /**
         * Convert private key to PEM format
         */
        public String privateKeyToPem(PrivateKey privateKey) throws Exception {
                StringWriter sw = new StringWriter();
                try (PemWriter pemWriter = new PemWriter(sw)) {
                        pemWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
                }
                return sw.toString();
        }

        /**
         * Generate CSR for ML-DSA
         */
        public PKCS10CertificationRequest generateCsr(KeyPair keyPair, String subjectDn, MlDsaLevel level)
                        throws Exception {
                X500Name subject = new X500Name(subjectDn);
                PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                                subject, keyPair.getPublic());

                ContentSigner signer = new JcaContentSignerBuilder(level.getAlgorithmName())
                                .setProvider("BCPQC")
                                .build(keyPair.getPrivate());

                return p10Builder.build(signer);
        }

        /**
         * Convert CSR to PEM format
         */
        public String csrToPem(PKCS10CertificationRequest csr) throws Exception {
                StringWriter sw = new StringWriter();
                try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
                        pemWriter.writeObject(csr);
                }
                return sw.toString();
        }

        /**
         * Generate secure serial number (RFC 5280 compliant)
         */
        public String generateSecureSerialNumber() {
                return new BigInteger(128, new SecureRandom()).toString(16).toUpperCase();
        }

        /**
         * Parse PEM-encoded CSR
         */
        public PKCS10CertificationRequest parseCsrPem(String csrPem) throws Exception {
                java.io.StringReader sr = new java.io.StringReader(csrPem);
                org.bouncycastle.openssl.PEMParser parser = new org.bouncycastle.openssl.PEMParser(sr);
                Object obj = parser.readObject();
                parser.close();

                if (obj instanceof PKCS10CertificationRequest) {
                        return (PKCS10CertificationRequest) obj;
                }
                throw new IllegalArgumentException("Not a valid CSR PEM");
        }

        /**
         * Extract Public Key from CSR
         */
        public PublicKey getPublicKeyFromCsr(PKCS10CertificationRequest csr) throws Exception {
                org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter converter = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                                .setProvider("BCPQC");
                return converter.getPublicKey(csr.getSubjectPublicKeyInfo());
        }

        /**
         * Extract Subject DN from CSR
         */
        public String getSubjectDnFromCsr(PKCS10CertificationRequest csr) {
                return csr.getSubject().toString();
        }
}
