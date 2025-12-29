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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Software-based Key Storage Service for DEVELOPMENT ONLY.
 * 
 * WARNING: Keys stored in memory are lost on restart.
 * This is NOT suitable for production use.
 * 
 * For production, use HsmKeyStorageService with proper HSM.
 */
@Service("softwareKeyStorage")
@Profile("dev")
public class SoftwareKeyStorageService implements KeyStorageService {

    private static final Logger log = LoggerFactory.getLogger(SoftwareKeyStorageService.class);
    private static final String ECDSA_ALGORITHM = "SHA384withECDSA";
    private static final String EC_CURVE = "secp384r1";

    // In-memory key storage - NOT FOR PRODUCTION
    private final Map<String, KeyPair> keys = new ConcurrentHashMap<>();

    public SoftwareKeyStorageService() {
        log.warn("============================================================");
        log.warn("  SOFTWARE KEY STORAGE ACTIVE - DEVELOPMENT MODE ONLY");
        log.warn("  Keys are stored in memory and will be lost on restart.");
        log.warn("  DO NOT USE IN PRODUCTION - Enable HSM for production.");
        log.warn("============================================================");
    }

    @Override
    public String generateKeyPair(String alias, String algorithm) throws Exception {
        log.info("Generating software key pair: alias={}, algorithm={}", alias, algorithm);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec(EC_CURVE));
        KeyPair keyPair = kpg.generateKeyPair();

        keys.put(alias, keyPair);
        log.info("Key pair stored in memory: {}", alias);

        return publicKeyToPem(keyPair.getPublic());
    }

    @Override
    public String signHash(String keyAlias, String dataHashBase64, String algorithm) throws Exception {
        log.info("Software signing with key: {}", keyAlias);

        KeyPair keyPair = keys.get(keyAlias);
        if (keyPair == null) {
            throw new IllegalArgumentException("Key not found in software storage: " + keyAlias);
        }

        byte[] dataHash = Base64.getDecoder().decode(dataHashBase64);

        Signature signature = Signature.getInstance(ECDSA_ALGORITHM, "BC");
        signature.initSign(keyPair.getPrivate());
        signature.update(dataHash);
        byte[] sig = signature.sign();

        return Base64.getEncoder().encodeToString(sig);
    }

    @Override
    public String generateCsr(String alias, String subject) throws Exception {
        log.info("Generating CSR for key: {}", alias);

        KeyPair keyPair = keys.get(alias);
        if (keyPair == null) {
            throw new IllegalArgumentException("Key not found: " + alias);
        }

        X500Name x500Subject = new X500Name(subject);
        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(
                x500Subject, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(ECDSA_ALGORITHM).build(keyPair.getPrivate());
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
    public boolean hasKey(String alias) {
        return keys.containsKey(alias);
    }
}
