package com.gov.crypto.caauthority.util;

import com.gov.crypto.common.pqc.PqcCryptoService;
import org.junit.jupiter.api.Test;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class SignatureDemoTest {

    @Test
    public void generateSignature() throws Exception {
        PqcCryptoService pqcService = new PqcCryptoService();

        // Read user private key
        String privateKeyPem = Files.readString(Path.of("test_user_private.key"));
        PrivateKey privateKey = pqcService.parsePrivateKeyPem(privateKeyPem);

        // Read user certificate for verification debugging
        String userCertPem = Files.readString(Path.of("test_user_cert.pem"));

        // Data to sign
        String originalDoc = "This is a contract signed by test_browser_user";
        byte[] docBytes = originalDoc.getBytes(StandardCharsets.UTF_8);

        // Hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(docBytes);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        // Sign the HASH, not the docBytes, because ValidationService expects to verify
        // the hash
        System.out.println("Signing data (hash) with ML-DSA-44...");
        byte[] signature = pqcService.sign(hash, privateKey, PqcCryptoService.MlDsaLevel.ML_DSA_44);
        String signatureBase64 = Base64.getEncoder().encodeToString(signature);

        System.out.println("--- VERIFY REQUEST DATA ---");
        System.out.println("OriginalDocHash: " + hashBase64);
        System.out.println("SignatureBase64: " + signatureBase64);
        System.out.println("---------------------------");

        // Create JSON payload
        String json = String.format(
                "{\n  \"OriginalDocHash\": \"%s\",\n  \"SignatureBase64\": \"%s\",\n  \"CertPem\": \"%s\"\n}",
                hashBase64, signatureBase64, userCertPem.replace("\n", "\\n").replace("\r", ""));
        // Wait, JSON escaping for CertPem which contains newlines is tricky in
        // String.format
        // Using python to generate valid JSON was safer.
        // But for local verification test, I don't need JSON to work perfectly, I need
        // local verify to work.
        // But I AM writing to verify.json for curl.
        // I will use simplified JSON construction or just trust I escape it roughly.
        // The verify.json logic: I'll use simple newline replacement.

        System.out.println(json);

        // Write to verify.json (simplified, curl accepts @file)
        // I need to properly format CertPem string in JSON (replace newlines with \n
        // literal).
        String escapedCertPem = userCertPem.replace("\n", "\\n").replace("\r", "");
        String validJson = "{\n" +
                "  \"originalDocHash\": \"" + hashBase64 + "\",\n" +
                "  \"signatureBase64\": \"" + signatureBase64 + "\",\n" +
                "  \"certPem\": \"" + escapedCertPem + "\"\n" +
                "}";

        try (java.io.FileWriter fw = new java.io.FileWriter("verify.json")) {
            fw.write(validJson);
            System.out.println("Wrote payload to verify.json");
        }

        // --- LOCAL VERIFICATION DEBUGGING ---
        try {
            System.out.println("Debugging verification locally...");
            X509Certificate cert = pqcService.parseCertificatePem(userCertPem);
            PublicKey pubKey = cert.getPublicKey();
            System.out.println("Public Key Algo: " + pubKey.getAlgorithm());

            boolean valid = pqcService.verify(hash, signature, pubKey, PqcCryptoService.MlDsaLevel.ML_DSA_44);
            System.out.println("Local Verification Result: " + valid);
        } catch (Exception e) {
            System.out.println("Local Verification Failed with Exception: " + e);
            e.printStackTrace();
        }
        // ------------------------------------

        // Write to file for easy usage
        Files.writeString(Path.of("signature_data.txt"),
                hashBase64 + "\n" + signatureBase64);
    }
}
