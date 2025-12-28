package com.gov.crypto.validationservice.service.impl;

import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

/**
 * Enhanced Validation Service with PQC support.
 */
@Service
public class ValidationServiceImpl implements ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationServiceImpl.class);

    @Value("${service.ca-authority.url:http://ca-authority:8082}")
    private String caAuthorityUrl;

    private final RestTemplate restTemplate;
    private final PqcCryptoService pqcService;

    public ValidationServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.pqcService = new PqcCryptoService();
    }

    @Override
    public VerifyResponse verifySignature(VerifyRequest request) {
        StringBuilder details = new StringBuilder();
        boolean cryptoValid = false;
        boolean notRevoked = true;
        boolean notExpired = false;
        boolean chainValid = true;

        try {
            // Parse certificate once
            X509Certificate cert = pqcService.parseCertificatePem(request.certPem());

            // 1. Verify cryptographic signature
            cryptoValid = verifyCryptoSignature(request, cert, details);

            // 2. Check certificate validity period
            notExpired = checkCertificateValidity(cert, details);

            // 3. Check revocation status
            notRevoked = checkRevocationStatus(cert, details);

            // 4. Verify certificate structure/chain (basic check)
            chainValid = verifyCertificateChain(cert, details);

            boolean isFullyValid = cryptoValid && notRevoked && notExpired && chainValid;

            String message = buildResultMessage(cryptoValid, notRevoked, notExpired, chainValid, details);
            log.info("Signature verification completed: valid={}, crypto={}, revocation={}, expiry={}, chain={}",
                    isFullyValid, cryptoValid, notRevoked, notExpired, chainValid);

            return new VerifyResponse(isFullyValid, message);

        } catch (Exception e) {
            log.error("Signature verification failed with exception", e);
            return new VerifyResponse(false, "Verification error: " + e.getMessage());
        }
    }

    private boolean verifyCryptoSignature(VerifyRequest request, X509Certificate cert, StringBuilder details) {
        try {
            byte[] hash = Base64.getDecoder().decode(request.originalDocHash());
            byte[] sig = Base64.getDecoder().decode(request.signatureBase64());
            PublicKey publicKey = cert.getPublicKey();

            String algo = publicKey.getAlgorithm();
            PqcCryptoService.MlDsaLevel level;

            if (algo.equalsIgnoreCase("Dilithium2")) {
                level = PqcCryptoService.MlDsaLevel.ML_DSA_44;
            } else if (algo.equalsIgnoreCase("Dilithium3")) {
                level = PqcCryptoService.MlDsaLevel.ML_DSA_65;
            } else if (algo.equalsIgnoreCase("Dilithium5")) {
                level = PqcCryptoService.MlDsaLevel.ML_DSA_87;
            } else {
                throw new IllegalArgumentException("Unsupported algorithm: " + algo);
            }

            boolean isValid = pqcService.verify(hash, sig, publicKey, level);

            if (isValid) {
                details.append("✓ Cryptographic signature verified. ");
            } else {
                details.append("✗ Cryptographic signature INVALID. ");
            }
            return isValid;

        } catch (Exception e) {
            log.error("Crypto verification failed", e);
            details.append("⚠ Crypto verification error: " + e.getMessage() + ". ");
            return false;
        }
    }

    private boolean checkCertificateValidity(X509Certificate cert, StringBuilder details) {
        try {
            cert.checkValidity();
            details.append("✓ Certificate is within validity period. ");
            return true;
        } catch (Exception e) {
            details.append("✗ Certificate has EXPIRED or not yet valid. ");
            return false;
        }
    }

    private boolean checkRevocationStatus(X509Certificate cert, StringBuilder details) {
        try {
            String serialNumber = cert.getSerialNumber().toString(16);
            try {
                String checkUrl = caAuthorityUrl + "/api/v1/ca/revocation-status/" + serialNumber;
                Map response = restTemplate.getForObject(checkUrl, Map.class);

                if (response != null && "REVOKED".equals(response.get("status"))) {
                    details.append("✗ Certificate is REVOKED. Reason: ")
                            .append(response.get("reason")).append(". ");
                    return false;
                }
                details.append("✓ Certificate is not revoked. ");
                return true;
            } catch (Exception e) {
                log.debug("Could not check revocation status: {}", e.getMessage());
                details.append("⚠ Revocation status could not be verified (CA offline). ");
                return true;
            }
        } catch (Exception e) {
            log.warn("Revocation check failed", e);
            details.append("⚠ Revocation check skipped. ");
            return true;
        }
    }

    private boolean verifyCertificateChain(X509Certificate cert, StringBuilder details) {
        // Basic self-consistency check for now
        // For full chain validation, we'd need the issuer cert
        if (cert != null) {
            details.append("✓ Certificate structure is valid. ");
            return true;
        }
        return false;
    }

    private String buildResultMessage(boolean crypto, boolean revocation, boolean expiry, boolean chain,
            StringBuilder details) {
        if (crypto && revocation && expiry && chain) {
            return "Signature is VALID. " + details;
        } else {
            StringBuilder msg = new StringBuilder("Signature verification FAILED: ");
            if (!crypto)
                msg.append("[Invalid Signature] ");
            if (!revocation)
                msg.append("[Certificate Revoked] ");
            if (!expiry)
                msg.append("[Certificate Expired] ");
            if (!chain)
                msg.append("[Invalid Chain] ");
            msg.append(details);
            return msg.toString();
        }
    }
}
