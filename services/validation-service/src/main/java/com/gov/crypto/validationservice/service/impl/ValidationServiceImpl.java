package com.gov.crypto.validationservice.service.impl;

import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.ValidationService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class ValidationServiceImpl implements ValidationService {

    @Override
    public VerifyResponse verifySignature(VerifyRequest request) {
        try {
            // 1. Write original hash to temp file
            Path hashPath = Files.createTempFile("hash", ".bin");
            byte[] hash = Base64.getDecoder().decode(request.originalDocHash());
            Files.write(hashPath, hash);

            // 2. Write signature to temp file
            Path sigPath = Files.createTempFile("sig", ".bin");
            byte[] sig = Base64.getDecoder().decode(request.signatureBase64());
            Files.write(sigPath, sig);

            // 3. Write certificate to temp file
            Path certPath = Files.createTempFile("cert", ".pem");
            Files.writeString(certPath, request.certPem());

            // 4. Extract public key from certificate
            Path pubKeyPath = Files.createTempFile("pubkey", ".pem");
            ProcessBuilder pbExtract = new ProcessBuilder(
                    "openssl", "x509",
                    "-in", certPath.toString(),
                    "-pubkey", "-noout");
            pbExtract.redirectOutput(pubKeyPath.toFile());
            pbExtract.redirectErrorStream(true);
            Process extractProcess = pbExtract.start();
            extractProcess.waitFor(5, TimeUnit.SECONDS);

            // 5. Verify signature using openssl pkeyutl
            // openssl pkeyutl -verify -pubin -inkey <pubkey> -in <hash> -sigfile <sig>
            ProcessBuilder pbVerify = new ProcessBuilder(
                    "openssl", "pkeyutl",
                    "-verify",
                    "-pubin",
                    "-inkey", pubKeyPath.toString(),
                    "-in", hashPath.toString(),
                    "-sigfile", sigPath.toString());
            pbVerify.redirectErrorStream(true);
            Process verifyProcess = pbVerify.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(verifyProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            verifyProcess.waitFor(5, TimeUnit.SECONDS);
            boolean isValid = verifyProcess.exitValue() == 0
                    && output.toString().contains("Signature Verified Successfully");

            // Cleanup
            Files.deleteIfExists(hashPath);
            Files.deleteIfExists(sigPath);
            Files.deleteIfExists(certPath);
            Files.deleteIfExists(pubKeyPath);

            return new VerifyResponse(isValid, isValid ? "Signature verified." : "Verification failed: " + output);

        } catch (Exception e) {
            return new VerifyResponse(false, "Error: " + e.getMessage());
        }
    }
}
