package com.gov.crypto.cloudsign.service.impl;

import com.gov.crypto.service.KeyStorageService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Date;

/**
 * Software-based Key Storage Service with ML-DSA (FIPS 204) support.
 * 
 * Supports:
 * - ML-DSA-44 (NIST Level 2, 128-bit security)
 * - ML-DSA-65 (NIST Level 3, 192-bit security)
 * - ML-DSA-87 (NIST Level 5, 256-bit security)
 * - EC (secp384r1, fallback for compatibility)
 * 
 * Keys are persisted to encrypted PKCS#12 keystore.
 */
@Service("softwareKeyStorage")
@Profile({ "dev", "kubernetes", "default" })
public class SoftwareKeyStorageService implements KeyStorageService {

    private static final Logger log = LoggerFactory.getLogger(SoftwareKeyStorageService.class);
    private static final String KEYSTORE_TYPE = "PKCS12";

    // Default to ML-DSA-65 (NIST Level 3) for citizen signing
    private static final String DEFAULT_ALGORITHM = "ML-DSA-65";

    @Value("${app.keystore.path:/tmp/keys/cloud-sign.p12}")
    private String keystorePath;

    @Value("${app.keystore.password:changeit}")
    private String keystorePassword;

    private KeyStore keyStore;

    public SoftwareKeyStorageService() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @jakarta.annotation.PostConstruct
    public void init() throws Exception {
        log.info("Initializing ML-DSA KeyStore at: {}", keystorePath);

        Path path = Path.of(keystorePath);
        Files.createDirectories(path.getParent());

        keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                keyStore.load(is, keystorePassword.toCharArray());
                log.info("Loaded existing keystore with {} entries", keyStore.size());
            }
        } else {
            keyStore.load(null, keystorePassword.toCharArray());
            saveKeyStore();
            log.info("Created new PKCS#12 keystore");
        }

        log.info("============================================================");
        log.info("  ML-DSA KEY STORAGE ACTIVE (BouncyCastle 1.83)");
        log.info("  Supported: ML-DSA-44, ML-DSA-65, ML-DSA-87, EC");
        log.info("  Keystore: {}", keystorePath);
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
        // Normalize algorithm - default to ML-DSA-65 if not specified
        String normalizedAlgo = normalizeAlgorithm(algorithm);
        log.info("Generating {} key pair: alias={}", normalizedAlgo, alias);

        // Check if key already exists
        if (keyStore.containsAlias(alias)) {
            log.info("Key already exists: {}", alias);
            return publicKeyToPem(keyStore.getCertificate(alias).getPublicKey());
        }

        KeyPair keyPair;
        String signatureAlgorithm;

        if (normalizedAlgo.startsWith("ML-DSA")) {
            // ML-DSA key generation
            keyPair = generateMlDsaKeyPair(normalizedAlgo);
            signatureAlgorithm = normalizedAlgo;
        } else {
            // EC fallback
            keyPair = generateEcKeyPair();
            signatureAlgorithm = "SHA384withECDSA";
        }

        // Create self-signed certificate for PKCS#12 storage
        X509Certificate cert = generateSelfSignedCert(keyPair, alias, signatureAlgorithm);

        // Store in keystore
        keyStore.setKeyEntry(
                alias,
                keyPair.getPrivate(),
                keystorePassword.toCharArray(),
                new Certificate[] { cert });

        saveKeyStore();
        log.info("{} key pair persisted: {}", normalizedAlgo, alias);

        return publicKeyToPem(keyPair.getPublic());
    }

    private String normalizeAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            return DEFAULT_ALGORITHM;
        }
        String upper = algorithm.toUpperCase().trim();
        return switch (upper) {
            case "ML-DSA-44", "MLDSA44", "DILITHIUM2" -> "ML-DSA-44";
            case "ML-DSA-65", "MLDSA65", "DILITHIUM3" -> "ML-DSA-65";
            case "ML-DSA-87", "MLDSA87", "DILITHIUM5" -> "ML-DSA-87";
            case "EC", "ECDSA", "SECP384R1" -> "EC";
            default -> DEFAULT_ALGORITHM;
        };
    }

    private KeyPair generateMlDsaKeyPair(String algorithm) throws Exception {
        MLDSAParameterSpec spec = switch (algorithm) {
            case "ML-DSA-44" -> MLDSAParameterSpec.ml_dsa_44;
            case "ML-DSA-65" -> MLDSAParameterSpec.ml_dsa_65;
            case "ML-DSA-87" -> MLDSAParameterSpec.ml_dsa_87;
            default -> MLDSAParameterSpec.ml_dsa_65;
        };

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA", "BC");
        kpg.initialize(spec, new SecureRandom());
        return kpg.generateKeyPair();
    }

    private KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec("secp384r1"), new SecureRandom());
        return kpg.generateKeyPair();
    }

    private X509Certificate generateSelfSignedCert(KeyPair keyPair, String alias, String signatureAlgorithm)
            throws Exception {
        X500Name subject = new X500Name("CN=" + alias + ",O=CloudSign,C=VN");

        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000 * 10); // 10 years

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));
    }

    @Override
    public synchronized String signHash(String keyAlias, String dataHashBase64, String algorithm) throws Exception {
        log.info("Signing with key: {}", keyAlias);

        if (!keyStore.containsAlias(keyAlias)) {
            throw new IllegalArgumentException("Key not found: " + keyAlias);
        }

        Key key = keyStore.getKey(keyAlias, keystorePassword.toCharArray());
        if (!(key instanceof PrivateKey privateKey)) {
            throw new IllegalArgumentException("Not a private key: " + keyAlias);
        }

        byte[] dataHash = Base64.getDecoder().decode(dataHashBase64);

        // Determine signature algorithm based on key type
        String sigAlgo = determineSignatureAlgorithm(privateKey);

        Signature signature = Signature.getInstance(sigAlgo, "BC");
        signature.initSign(privateKey);
        signature.update(dataHash);
        byte[] sig = signature.sign();

        log.info("Signature created with {}: {}", sigAlgo, keyAlias);
        return Base64.getEncoder().encodeToString(sig);
    }

    private String determineSignatureAlgorithm(PrivateKey privateKey) {
        String keyAlgo = privateKey.getAlgorithm();
        if (keyAlgo.contains("ML-DSA") || keyAlgo.contains("Dilithium")) {
            // For ML-DSA, use the algorithm directly
            return keyAlgo;
        } else if (keyAlgo.equals("EC") || keyAlgo.equals("ECDSA")) {
            return "SHA384withECDSA";
        }
        return keyAlgo;
    }

    @Override
    public synchronized String generateCsr(String alias, String subject) throws Exception {
        log.info("Generating CSR for key: {}", alias);

        if (!keyStore.containsAlias(alias)) {
            throw new IllegalArgumentException("Key not found: " + alias);
        }

        Key key = keyStore.getKey(alias, keystorePassword.toCharArray());
        if (!(key instanceof PrivateKey privateKey)) {
            throw new IllegalArgumentException("Not a private key: " + alias);
        }

        PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
        String sigAlgo = determineSignatureAlgorithm(privateKey);

        X500Name x500Subject = new X500Name(subject);
        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(
                x500Subject, publicKey);

        ContentSigner signer = new JcaContentSignerBuilder(sigAlgo)
                .setProvider("BC")
                .build(privateKey);
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

    public synchronized boolean hasKey(String alias) {
        try {
            return keyStore.containsAlias(alias);
        } catch (KeyStoreException e) {
            log.error("Error checking key: {}", e.getMessage());
            return false;
        }
    }
}
