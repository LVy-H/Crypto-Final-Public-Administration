package com.gov.crypto.cloudsign.service.impl;

import com.gov.crypto.service.KeyStorageService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Date;

/**
 * Software-based Key Storage Service using PKCS#12 KeyStore.
 * 
 * SECURITY IMPROVEMENT (2026): Keys are now persisted to disk using
 * encrypted PKCS#12 format instead of volatile in-memory storage.
 * 
 * Keys survive pod restarts when backed by a PersistentVolumeClaim.
 * 
 * For production, still recommend HsmKeyStorageService with proper HSM.
 */
@Service("softwareKeyStorage")
@Profile("dev")
public class SoftwareKeyStorageService implements KeyStorageService {

    private static final Logger log = LoggerFactory.getLogger(SoftwareKeyStorageService.class);
    private static final String ECDSA_ALGORITHM = "SHA384withECDSA";
    private static final String EC_CURVE = "secp384r1";
    private static final String KEYSTORE_TYPE = "PKCS12";

    @Value("${app.keystore.path:/data/keys/cloud-sign.p12}")
    private String keystorePath;

    @Value("${app.keystore.password:changeit}")
    private String keystorePassword;

    private KeyStore keyStore;

    public SoftwareKeyStorageService() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @jakarta.annotation.PostConstruct
    public void init() throws Exception {
        log.info("Initializing PKCS#12 KeyStore at: {}", keystorePath);

        Path path = Path.of(keystorePath);
        Files.createDirectories(path.getParent());

        keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

        if (Files.exists(path)) {
            // Load existing keystore
            try (InputStream is = Files.newInputStream(path)) {
                keyStore.load(is, keystorePassword.toCharArray());
                log.info("Loaded existing keystore with {} entries", keyStore.size());
            }
        } else {
            // Create new empty keystore
            keyStore.load(null, keystorePassword.toCharArray());
            saveKeyStore();
            log.info("Created new PKCS#12 keystore");
        }

        log.info("============================================================");
        log.info("  PKCS#12 KEY STORAGE ACTIVE - Keys persist across restarts");
        log.info("  Keystore: {}", keystorePath);
        log.info("  For production, enable HSM via HsmKeyStorageService.");
        log.info("============================================================");
    }

    private synchronized void saveKeyStore() throws Exception {
        Path path = Path.of(keystorePath);
        try (OutputStream os = Files.newOutputStream(path)) {
            keyStore.store(os, keystorePassword.toCharArray());
        }
    }

    @Override
    public synchronized String generateKeyPair(String alias, String algorithm) throws Exception {
        log.info("Generating key pair: alias={}, algorithm={}", alias, algorithm);

        // Check if key already exists
        if (keyStore.containsAlias(alias)) {
            log.info("Key already exists: {}", alias);
            return publicKeyToPem((PublicKey) keyStore.getCertificate(alias).getPublicKey());
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec(EC_CURVE));
        KeyPair keyPair = kpg.generateKeyPair();

        // Create a self-signed certificate for the key (required for PKCS#12)
        X509Certificate cert = generateSelfSignedCert(keyPair, alias);

        // Store in keystore
        keyStore.setKeyEntry(
                alias,
                keyPair.getPrivate(),
                keystorePassword.toCharArray(),
                new Certificate[] { cert });

        saveKeyStore();
        log.info("Key pair persisted to PKCS#12: {}", alias);

        return publicKeyToPem(keyPair.getPublic());
    }

    private X509Certificate generateSelfSignedCert(KeyPair keyPair, String alias) throws Exception {
        // Generate a simple self-signed certificate for key storage purposes
        org.bouncycastle.asn1.x500.X500Name subject = new org.bouncycastle.asn1.x500.X500Name(
                "CN=" + alias + ",O=CloudSign Key Storage,C=VN");

        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000 * 10); // 10 years

        org.bouncycastle.cert.X509v3CertificateBuilder certBuilder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(ECDSA_ALGORITHM)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        return new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));
    }

    @Override
    public synchronized String signHash(String keyAlias, String dataHashBase64, String algorithm) throws Exception {
        log.info("Signing with key: {}", keyAlias);

        if (!keyStore.containsAlias(keyAlias)) {
            throw new IllegalArgumentException("Key not found in keystore: " + keyAlias);
        }

        Key key = keyStore.getKey(keyAlias, keystorePassword.toCharArray());
        if (!(key instanceof PrivateKey)) {
            throw new IllegalArgumentException("Entry is not a private key: " + keyAlias);
        }

        byte[] dataHash = Base64.getDecoder().decode(dataHashBase64);

        Signature signature = Signature.getInstance(ECDSA_ALGORITHM, "BC");
        signature.initSign((PrivateKey) key);
        signature.update(dataHash);
        byte[] sig = signature.sign();

        return Base64.getEncoder().encodeToString(sig);
    }

    @Override
    public synchronized String generateCsr(String alias, String subject) throws Exception {
        log.info("Generating CSR for key: {}", alias);

        if (!keyStore.containsAlias(alias)) {
            throw new IllegalArgumentException("Key not found: " + alias);
        }

        Key key = keyStore.getKey(alias, keystorePassword.toCharArray());
        if (!(key instanceof PrivateKey)) {
            throw new IllegalArgumentException("Entry is not a private key: " + alias);
        }

        PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();

        X500Name x500Subject = new X500Name(subject);
        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(
                x500Subject, publicKey);

        ContentSigner signer = new JcaContentSignerBuilder(ECDSA_ALGORITHM).build((PrivateKey) key);
        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded()));
        }

        return sw.toString();
    }

    private String publicKeyToPem(PublicKey publicKey) throws Exception {
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        }
        return sw.toString();
    }

    /**
     * Check if a key exists.
     */
    public synchronized boolean hasKey(String alias) {
        try {
            return keyStore.containsAlias(alias);
        } catch (KeyStoreException e) {
            log.error("Error checking key existence: {}", e.getMessage());
            return false;
        }
    }
}
