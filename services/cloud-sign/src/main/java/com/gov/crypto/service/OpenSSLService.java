package com.gov.crypto.cloudsign.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class OpenSSLService {

    // Placeholder for Key Storage Path
    private static final String KEY_STORAGE_PATH = "/secure/keys";

    /**
     * Signs a data hash using a specific private key alias and algorithm.
     * Supports PQC algorithms accessible via OpenSSL 3.6+ (e.g., Dilithium5).
     */
    public String signHash(String keyAlias, String dataHashBase64, String algorithm) throws Exception {
        // 1. Prepare temporary input file with hash
        Path inputPath = Files.createTempFile("data", ".hash");
        byte[] dataHash = Base64.getDecoder().decode(dataHashBase64);
        Files.write(inputPath, dataHash);

        Path signaturePath = Files.createTempFile("sig", ".bin");

        try {
            // 2. Build OpenSSL Command
            // openssl pkeyutl -sign -inkey <key_path> -in <hash_file> -out <sig_file>
            // -pkeyopt digest:sha256
            // Note: For PQC, algorithms are often implicitly defined by the key type in
            // OpenSSL 3.6
            ProcessBuilder pb = new ProcessBuilder(
                    "openssl", "pkeyutl",
                    "-sign",
                    "-inkey", KEY_STORAGE_PATH + "/" + keyAlias + ".pem",
                    "-in", inputPath.toString(),
                    "-out", signaturePath.toString());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output for debugging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new RuntimeException("OpenSSL signing failed: " + output);
            }

            // 3. Read Signature and Return Base64
            byte[] signatureBytes = Files.readAllBytes(signaturePath);
            return Base64.getEncoder().encodeToString(signatureBytes);

        } finally {
            // Cleanup
            Files.deleteIfExists(inputPath);
            Files.deleteIfExists(signaturePath);
        }
    }

    private void ensureKeyStorage() {
        File dir = new File(KEY_STORAGE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public String generateKeyPair(String alias, String algorithm) throws Exception {
        ensureKeyStorage();
        // 1. Generate Private Key
        // openssl genpkey -algorithm <algo> -out <key_path>
        ProcessBuilder pb = new ProcessBuilder(
                "openssl", "genpkey",
                "-algorithm", algorithm,
                "-out", KEY_STORAGE_PATH + "/" + alias + ".pem");
        runProcess(pb, "Key Generation");

        // 2. Extract Public Key
        // openssl pkey -in <private_key> -pubout
        ProcessBuilder pbPub = new ProcessBuilder(
                "openssl", "pkey",
                "-in", KEY_STORAGE_PATH + "/" + alias + ".pem",
                "-pubout");
        return runProcess(pbPub, "Public Key Extraction");
    }

    private String runProcess(ProcessBuilder pb, String operation) throws Exception {
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new RuntimeException("OpenSSL " + operation + " failed: " + output);
        }
        return output.toString();
    }

    public String generateCsr(String alias, String subject) throws Exception {
        ensureKeyStorage();
        // openssl req -new -key <key> -out <csr> -subj <subject>
        Path csrPath = Files.createTempFile("request", ".csr");

        ProcessBuilder pb = new ProcessBuilder(
                "openssl", "req", "-new",
                "-key", KEY_STORAGE_PATH + "/" + alias + ".pem",
                "-out", csrPath.toString(),
                "-subj", subject);
        runProcess(pb, "CSR Generation");

        try {
            return Files.readString(csrPath);
        } finally {
            Files.deleteIfExists(csrPath);
        }
    }
}
