package com.gov.crypto.validationservice.service.impl;

import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.validationservice.dto.StampVerifyRequest;
import com.gov.crypto.validationservice.dto.StampVerifyResponse;
import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import com.gov.crypto.common.validation.DssCertificateVerifier;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.validation.CertificateValidator;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import eu.europa.esig.dss.simplecertificatereport.SimpleCertificateReport;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SubIndication;
import java.util.List;

/**
 * Enhanced Validation Service with PQC support.
 */
@Service
public class ValidationServiceImpl implements ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationServiceImpl.class);

    private final PqcCryptoService pqcService;
    private final DssCertificateVerifier certificateVerifier;

    @Autowired
    public ValidationServiceImpl(PqcCryptoService pqcService, DssCertificateVerifier certificateVerifier) {
        this.pqcService = pqcService;
        this.certificateVerifier = certificateVerifier;
    }

    @Override
    public VerifyResponse verifySignature(VerifyRequest request) {
        StringBuilder details = new StringBuilder();
        boolean cryptoValid = false;

        try {
            // Parse certificate
            X509Certificate cert = pqcService.parseCertificatePem(request.certPem());

            // 1. Verify cryptographic signature
            cryptoValid = verifyCryptoSignature(request, cert, details);

            // 2. Perform robust certificate validation using SD-DSS
            CertificateToken token = DSSUtils.loadCertificate(cert.getEncoded());
            CertificateValidator validator = CertificateValidator.fromCertificate(token);
            validator.setCertificateVerifier(certificateVerifier);

            CertificateReports reports = validator.validate();
            SimpleCertificateReport simpleReport = reports.getSimpleReport();

            List<String> certificateIds = simpleReport.getCertificateIds();
            String indicationStr = "UNKNOWN";
            boolean dssValid = false;

            if (certificateIds != null && !certificateIds.isEmpty()) {
                String certId = certificateIds.get(0);
                Indication indication = simpleReport.getCertificateIndication(certId);
                SubIndication subIndication = simpleReport.getCertificateSubIndication(certId);

                indicationStr = indication != null ? indication.name() : "NULL";
                if (subIndication != null) {
                    indicationStr += " (" + subIndication.name() + ")";
                }

                dssValid = Indication.TOTAL_PASSED.equals(indication);
            }

            details.append(" Certificate Status: ").append(indicationStr).append(". ");

            boolean isFullyValid = cryptoValid && dssValid;

            if (isFullyValid) {
                return new VerifyResponse(true, "Signature is VALID. " + details);
            } else {
                return new VerifyResponse(false, "Signature validation FAILED: " + details);
            }
        } catch (Exception e) {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                java.nio.file.Files.write(java.nio.file.Paths.get("exception.txt"), sw.toString().getBytes());
            } catch (Exception ex) {
                // ignore
            }
            log.error("Signature verification failed with exception", e);
            return new VerifyResponse(false, "Verification error: " + e.getMessage());
        }
    }

    // Kept for document signature reference
    private boolean verifyCryptoSignature(VerifyRequest request, X509Certificate cert, StringBuilder details) {
        try {
            byte[] hash = Base64.getDecoder().decode(request.originalDocHash());
            byte[] sig = Base64.getDecoder().decode(request.signatureBase64());
            PublicKey publicKey = cert.getPublicKey();

            String algo = publicKey.getAlgorithm();
            PqcCryptoService.MlDsaLevel level;

            if (algo.equalsIgnoreCase("Dilithium2") || algo.contains("44")) {
                level = PqcCryptoService.MlDsaLevel.ML_DSA_44;
            } else if (algo.equalsIgnoreCase("Dilithium3") || algo.contains("65")) {
                level = PqcCryptoService.MlDsaLevel.ML_DSA_65;
            } else if (algo.equalsIgnoreCase("Dilithium5") || algo.contains("87")) {
                level = PqcCryptoService.MlDsaLevel.ML_DSA_87;
            } else {
                level = PqcCryptoService.MlDsaLevel.ML_DSA_65; // fallback or error
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

    // Removed manual check methods (checkCertificateValidity,
    // checkRevocationStatus, verifyCertificateChain)
    // as they are replaced by SD-DSS logic.

    @Override
    public StampVerifyResponse verifyStamp(StampVerifyRequest request) {
        StringBuilder details = new StringBuilder();
        boolean userSigValid = false;
        boolean officerSigValid = false;
        boolean timestampValid = true; // Optional, default to true if not provided
        boolean userCertValid = false;
        boolean officerCertValid = false;

        try {
            // 1. Parse and validate user certificate
            X509Certificate userCert = pqcService.parseCertificatePem(request.userCertPem());
            try {
                userCert.checkValidity();
                userCertValid = true;
                details.append("✓ User certificate is valid. ");
            } catch (Exception e) {
                details.append("✗ User certificate expired/invalid. ");
            }

            // 2. Verify user's signature on document
            byte[] docHash = Base64.getDecoder().decode(request.documentHash());
            byte[] userSig = Base64.getDecoder().decode(request.userSignature());
            PqcCryptoService.MlDsaLevel userLevel = getMlDsaLevel(userCert.getPublicKey().getAlgorithm());

            userSigValid = pqcService.verify(docHash, userSig, userCert.getPublicKey(), userLevel);
            if (userSigValid) {
                details.append("✓ User signature verified. ");
            } else {
                details.append("✗ User signature INVALID. ");
            }

            // 3. Parse and validate officer certificate
            X509Certificate officerCert = pqcService.parseCertificatePem(request.officerCertPem());
            try {
                officerCert.checkValidity();
                officerCertValid = true;
                details.append("✓ Officer certificate is valid. ");
            } catch (Exception e) {
                details.append("✗ Officer certificate expired/invalid. ");
            }

            // 4. Verify officer's countersignature
            // Officer signs: SHA256(documentHash + ":" + userSignature)
            String stampData = request.documentHash() + ":" + request.userSignature();
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] stampHash = digest.digest(stampData.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] officerSig = Base64.getDecoder().decode(request.officerSignature());
            PqcCryptoService.MlDsaLevel officerLevel = getMlDsaLevel(officerCert.getPublicKey().getAlgorithm());

            officerSigValid = pqcService.verify(stampHash, officerSig, officerCert.getPublicKey(), officerLevel);
            if (officerSigValid) {
                details.append("✓ Officer countersignature verified. ");
            } else {
                details.append("✗ Officer countersignature INVALID. ");
            }

            // 5. Verify timestamp (if provided)
            if (request.timestampToken() != null && !request.timestampToken().isEmpty()) {
                // Simplified timestamp verification - just check format
                // Full TSA verification would require the TSA certificate
                try {
                    byte[] tsToken = Base64.getDecoder().decode(request.timestampToken());
                    String tokenStr = new String(tsToken, java.nio.charset.StandardCharsets.UTF_8);
                    if (tokenStr.contains("genTime") && tokenStr.contains("hashedMessage")) {
                        timestampValid = true;
                        details.append("✓ Timestamp token present. ");
                    } else {
                        timestampValid = false;
                        details.append("⚠ Timestamp format unrecognized. ");
                    }
                } catch (Exception e) {
                    timestampValid = false;
                    details.append("⚠ Timestamp verification failed. ");
                }
            } else {
                details.append("ℹ No timestamp provided. ");
            }

            boolean allValid = userSigValid && officerSigValid && userCertValid && officerCertValid && timestampValid;
            String message = allValid ? "Stamp is VALID" : "Stamp verification FAILED";

            return new StampVerifyResponse(
                    allValid,
                    userSigValid,
                    officerSigValid,
                    timestampValid,
                    userCertValid,
                    officerCertValid,
                    message,
                    details.toString());

        } catch (Exception e) {
            log.error("Stamp verification failed", e);
            return new StampVerifyResponse(false, "Verification error: " + e.getMessage());
        }
    }

    private PqcCryptoService.MlDsaLevel getMlDsaLevel(String algorithm) {
        if (algorithm.equalsIgnoreCase("Dilithium2") || algorithm.contains("44")) {
            return PqcCryptoService.MlDsaLevel.ML_DSA_44;
        } else if (algorithm.equalsIgnoreCase("Dilithium3") || algorithm.contains("65")) {
            return PqcCryptoService.MlDsaLevel.ML_DSA_65;
        } else {
            return PqcCryptoService.MlDsaLevel.ML_DSA_87;
        }
    }

    @Override
    public String signDebug(String privateKeyPem, String dataBase64, String algorithm) {
        try {
            java.security.PrivateKey privateKey = pqcService.parsePrivateKeyPem(privateKeyPem);
            byte[] data = Base64.getDecoder().decode(dataBase64);
            PqcCryptoService.MlDsaLevel level = getMlDsaLevel(algorithm);

            byte[] signature = pqcService.sign(data, privateKey, level);
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            log.error("Debug signing failed", e);
            throw new RuntimeException("Debug signing failed: " + e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, String> generateCsrDebug(String subjectDn, String algorithm) {
        try {
            PqcCryptoService.MlDsaLevel level = getMlDsaLevel(algorithm);
            java.security.KeyPair keyPair = pqcService.generateMlDsaKeyPair(level);

            var csr = pqcService.generateCsr(keyPair, subjectDn, level);

            String privateKeyPem = pqcService.privateKeyToPem(keyPair.getPrivate());
            String publicKeyPem = pqcService.publicKeyToPem(keyPair.getPublic());
            String csrPem = pqcService.csrToPem(csr);

            return java.util.Map.of(
                    "privateKey", privateKeyPem,
                    "publicKey", publicKeyPem,
                    "csr", csrPem);
        } catch (Throwable e) {
            log.error("Debug CSR generation failed", e);
            System.err.println("CRITICAL ERROR: Debug CSR generation failed");
            e.printStackTrace();
            throw new RuntimeException("Debug CSR generation failed: " + e.getMessage());
        }
    }
}
