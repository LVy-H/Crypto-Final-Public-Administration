package com.gov.crypto.cloudsign.service.impl;

import com.gov.crypto.service.KeyStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * HSM-backed Key Storage Service using PKCS#11.
 * 
 * SECURITY: Private keys never leave the HSM.
 * Only key handles (aliases) are stored in the database.
 * 
 * For development: Use SoftwareKeyStorageService with @Profile("dev")
 * For production: This service with real HSM (Thales, Utimaco, etc.)
 */
@Service("hsmKeyStorage")
@Profile("prod")
public class HsmKeyStorageService implements KeyStorageService {

    private static final Logger log = LoggerFactory.getLogger(HsmKeyStorageService.class);
    private static final String ECDSA_ALGORITHM = "SHA384withECDSA";
    private static final String EC_CURVE = "secp384r1";

    private Provider pkcs11Provider;
    private KeyStore hsmKeyStore;
    private final char[] userPin;

    public HsmKeyStorageService(
            @Value("${hsm.library:/usr/lib/softhsm/libsofthsm2.so}") String hsmLibrary,
            @Value("${hsm.slot:0}") int hsmSlot,
            @Value("${hsm.user-pin:87654321}") String hsmUserPin,
            @Value("${hsm.token-label:gov-signing-token}") String tokenLabel) {

        this.userPin = hsmUserPin.toCharArray();

        try {
            initializePkcs11(hsmLibrary, hsmSlot, tokenLabel);
            log.info("HSM Key Storage initialized with PKCS#11");
        } catch (Exception e) {
            log.error("FAILED to initialize HSM - service will not function: {}", e.getMessage());
            throw new IllegalStateException("HSM initialization failed - cannot start in prod mode", e);
        }
    }

    private void initializePkcs11(String library, int slot, String tokenLabel) throws Exception {
        // Create PKCS#11 configuration
        String pkcs11Config = String.format("""
                name = SoftHSM
                library = %s
                slot = %d
                """, library, slot);

        // Load PKCS#11 provider
        pkcs11Provider = Security.getProvider("SunPKCS11");
        if (pkcs11Provider == null) {
            // Try to load dynamically
            pkcs11Provider = (Provider) Class.forName("sun.security.pkcs11.SunPKCS11")
                    .getConstructor(java.io.InputStream.class)
                    .newInstance(new ByteArrayInputStream(pkcs11Config.getBytes()));
            Security.addProvider(pkcs11Provider);
        } else {
            pkcs11Provider = pkcs11Provider.configure(pkcs11Config);
        }

        // Load HSM KeyStore
        hsmKeyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);
        hsmKeyStore.load(null, userPin);

        log.info("PKCS#11 provider loaded: {}", pkcs11Provider.getName());
    }

    @Override
    public String generateKeyPair(String alias, String algorithm) throws Exception {
        log.info("Generating key pair in HSM: alias={}, algorithm={}", alias, algorithm);
        return generateKeyInHsm(alias, algorithm);
    }

    private String generateKeyInHsm(String alias, String algorithm) throws Exception {
        // Generate key pair inside the HSM
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", pkcs11Provider);
        kpg.initialize(new ECGenParameterSpec(EC_CURVE));
        KeyPair keyPair = kpg.generateKeyPair();

        // Store in HSM - private key stays in HSM
        // Note: For PKCS#11, we typically need a certificate chain
        // For now, create a self-signed cert placeholder
        java.security.cert.X509Certificate selfSignedCert = createSelfSignedCertificate(keyPair, alias);
        Certificate[] certChain = new Certificate[] { selfSignedCert };

        hsmKeyStore.setKeyEntry(alias, keyPair.getPrivate(), userPin, certChain);

        log.info("Key pair generated in HSM with alias: {}", alias);

        // Return public key in PEM format
        return publicKeyToPem(keyPair.getPublic());
    }

    @Override
    public String signHash(String keyAlias, String dataHashBase64, String algorithm) throws Exception {
        log.info("Signing with HSM key: {}", keyAlias);
        byte[] dataHash = Base64.getDecoder().decode(dataHashBase64);
        return signWithHsm(keyAlias, dataHash);
    }

    private String signWithHsm(String alias, byte[] data) throws Exception {
        // Get private key from HSM (key never leaves HSM)
        PrivateKey privateKey = (PrivateKey) hsmKeyStore.getKey(alias, userPin);

        if (privateKey == null) {
            throw new IllegalArgumentException("Key not found in HSM: " + alias);
        }

        // Sign using HSM
        Signature signature = Signature.getInstance(ECDSA_ALGORITHM, pkcs11Provider);
        signature.initSign(privateKey);
        signature.update(data);
        byte[] sig = signature.sign();

        log.info("Signature created in HSM for key: {}", alias);
        return Base64.getEncoder().encodeToString(sig);
    }



    @Override
    public String generateCsr(String alias, String subject) throws Exception {
        log.info("Generating CSR for key: {}, subject: {}", alias, subject);

        PublicKey publicKey;
        PrivateKey privateKey;

        if (hsmKeyStore != null) {
            java.security.cert.Certificate cert = hsmKeyStore.getCertificate(alias);
            if (cert == null) {
                throw new IllegalArgumentException("Certificate not found for alias: " + alias);
            }
            publicKey = cert.getPublicKey();
            privateKey = (PrivateKey) hsmKeyStore.getKey(alias, userPin);
        } else {
            throw new IllegalStateException("HSM KeyStore not initialized");
        }

        // Generate CSR using BouncyCastle
        org.bouncycastle.asn1.x500.X500Name x500Subject = new org.bouncycastle.asn1.x500.X500Name(subject);
        org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder csrBuilder = new org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder(
                x500Subject, publicKey);

        org.bouncycastle.operator.ContentSigner signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(
                ECDSA_ALGORITHM)
                .build(privateKey);

        org.bouncycastle.pkcs.PKCS10CertificationRequest csr = csrBuilder.build(signer);

        // Convert to PEM
        StringWriter sw = new StringWriter();
        try (org.bouncycastle.util.io.pem.PemWriter pemWriter = new org.bouncycastle.util.io.pem.PemWriter(sw)) {
            pemWriter.writeObject(new org.bouncycastle.util.io.pem.PemObject(
                    "CERTIFICATE REQUEST", csr.getEncoded()));
        }

        return sw.toString();
    }

    private java.security.cert.X509Certificate createSelfSignedCertificate(KeyPair keyPair, String alias)
            throws Exception {
        // Create minimal self-signed certificate for PKCS#11 storage
        String dn = "CN=" + alias + ",O=GovCrypto,C=VN";

        org.bouncycastle.asn1.x500.X500Name subject = new org.bouncycastle.asn1.x500.X500Name(dn);
        java.math.BigInteger serial = java.math.BigInteger.valueOf(System.currentTimeMillis());
        java.util.Date notBefore = new java.util.Date();
        java.util.Date notAfter = new java.util.Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);

        org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder certBuilder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        org.bouncycastle.operator.ContentSigner signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(
                ECDSA_ALGORITHM)
                .build(keyPair.getPrivate());

        return new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
    }

    private String publicKeyToPem(PublicKey publicKey) throws Exception {
        StringWriter sw = new StringWriter();
        try (org.bouncycastle.util.io.pem.PemWriter pemWriter = new org.bouncycastle.util.io.pem.PemWriter(sw)) {
            pemWriter.writeObject(new org.bouncycastle.util.io.pem.PemObject(
                    "PUBLIC KEY", publicKey.getEncoded()));
        }
        return sw.toString();
    }
}
