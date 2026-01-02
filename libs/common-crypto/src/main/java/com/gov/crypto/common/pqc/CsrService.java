package com.gov.crypto.common.pqc;

import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.Security;

/**
 * Service for Certificate Signing Request (CSR) operations.
 */
@Service
public class CsrService {

    private static final Logger log = LoggerFactory.getLogger(CsrService.class);

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    private final PqcCryptoService pqcCryptoService;

    public CsrService(PqcCryptoService pqcCryptoService) {
        this.pqcCryptoService = pqcCryptoService;
    }

    /**
     * Validates the signature of a CSR.
     * Checks if the CSR was signed by the private key corresponding to the public
     * key contained within it.
     * 
     * @param csrPem The PEM encoded CSR string
     * @return true if signature is valid, false otherwise
     * @throws Exception if parsing fails or validation errors occur
     */
    public boolean validateSignature(String csrPem) throws Exception {
        PKCS10CertificationRequest csr = pqcCryptoService.parseCsrPem(csrPem);
        return validateSignature(csr);
    }

    /**
     * Validates the signature of a CSR.
     * 
     * @param csr The PKCS10CertificationRequest object
     * @return true if signature is valid, false otherwise
     * @throws Exception if validation errors occur
     */
    public boolean validateSignature(PKCS10CertificationRequest csr) throws Exception {
        try {
            PublicKey publicKey = pqcCryptoService.getPublicKeyFromCsr(csr);

            ContentVerifierProvider verifierProvider = new JcaContentVerifierProviderBuilder()
                    .setProvider("BC")
                    .build(publicKey);

            boolean isValid = csr.isSignatureValid(verifierProvider);

            if (isValid) {
                log.debug("CSR signature validation successful for subject: {}", csr.getSubject());
            } else {
                log.warn("CSR signature validation FAILED for subject: {}", csr.getSubject());
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating CSR signature: {}", e.getMessage(), e);
            throw e;
        }
    }
}
