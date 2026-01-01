package com.gov.crypto.offlineca;

import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService.MlDsaLevel;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

@Command(name = "offline-ca", mixinStandardHelpOptions = true, version = "1.0",
        description = "Offline Root CA Management Tool for ML-DSA (Post-Quantum) PKI",
        subcommands = { OfflineCaTool.InitRootCommand.class, OfflineCaTool.SignCsrCommand.class })
public class OfflineCaTool implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new OfflineCaTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "init-root", description = "Initialize a new ML-DSA Root CA")
    static class InitRootCommand implements Callable<Integer> {

        @Option(names = {"-n", "--name"}, description = "Subject DN (default: CN=National Root CA, O=Government)", defaultValue = "CN=National Root CA, O=Government")
        private String name;

        @Option(names = {"-a", "--algo"}, description = "Algorithm: ML_DSA_44, ML_DSA_65, ML_DSA_87 (default: ML_DSA_87)", defaultValue = "ML_DSA_87")
        private MlDsaLevel algo;

        @Option(names = {"-d", "--days"}, description = "Validity days (default: 7300 - 20 years)", defaultValue = "7300")
        private int days;

        @Option(names = {"-o", "--out-dir"}, description = "Output directory", required = true)
        private File outDir;

        @Override
        public Integer call() throws Exception {
            System.out.println("Initializing Root CA...");
            System.out.println("  DN: " + name);
            System.out.println("  Algo: " + algo);
            System.out.println("  Output: " + outDir.getAbsolutePath());

            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("Failed to create output directory");
                return 1;
            }

            PqcCryptoService pqc = new PqcCryptoService();

            // 1. Generate Keys
            KeyPair keyPair = pqc.generateMlDsaKeyPair(algo);

            // 2. Self-Sign Cert
            X509Certificate cert = pqc.generateSelfSignedCertificate(keyPair, name, days, algo);

            // 3. Save Private Key (WARNING: Plaintext for now - encrypt in production!)
            String privKeyPem = pqc.privateKeyToPem(keyPair.getPrivate());
            Files.writeString(Path.of(outDir.getAbsolutePath(), "root.key"), privKeyPem);

            // 4. Save Public Key
            String pubKeyPem = pqc.publicKeyToPem(keyPair.getPublic());
            Files.writeString(Path.of(outDir.getAbsolutePath(), "root.pub"), pubKeyPem);

            // 5. Save Cert
            String certPem = pqc.certificateToPem(cert);
            Files.writeString(Path.of(outDir.getAbsolutePath(), "root.crt"), certPem);

            System.out.println("SUCCESS: Root CA initialized.");
            System.out.println("  - root.key (PRIVATE! KEEP OFFLINE)");
            System.out.println("  - root.crt (Public Trusted Root)");
            return 0;
        }
    }

    @Command(name = "sign-csr", description = "Sign a Subordinate CA CSR")
    static class SignCsrCommand implements Callable<Integer> {

        @Option(names = {"--csr"}, description = "Path to CSR file", required = true)
        private File csrFile;

        @Option(names = {"--key"}, description = "Path to Root CA Private Key", required = true)
        private File keyFile;

        @Option(names = {"--cert"}, description = "Path to Root CA Certificate", required = true)
        private File certFile;

        @Option(names = {"--out"}, description = "Output path for signed certificate", required = true)
        private File outFile;

        @Option(names = {"-d", "--days"}, description = "Validity days (default: 1825 - 5 years)", defaultValue = "1825")
        private int days;

        @Override
        public Integer call() throws Exception {
            System.out.println("Signing Subordinate CA CSR...");

            PqcCryptoService pqc = new PqcCryptoService();

            // 1. Read Inputs
            String csrPem = Files.readString(csrFile.toPath());
            String keyPem = Files.readString(keyFile.toPath());
            String certPem = Files.readString(certFile.toPath());

            // 2. Parse Root Material
            PrivateKey rootKey = pqc.parsePrivateKeyPem(keyPem);
            X509Certificate rootCert = pqc.parseCertificatePem(certPem);
            
            // 3. Parse CSR to get details
            var csr = pqc.parseCsrPem(csrPem);
            var subjectDn = pqc.getSubjectDnFromCsr(csr);
            var pubKey = pqc.getPublicKeyFromCsr(csr);
            
            // NOTE: We assume the Root CA algo matches the Root Key (Dilithium levels matters!)
            // Ideally we detect from key, but here we assume the PqcCryptoService handles signer builder correctly 
            // if we use the correct OID. 
            // Actually PqcCryptoService requires 'level' param for signer.
            // We need to determine the level from the Root Key or Cert.
            
            // Heuristic: Check Root Cert Sig Algo OID? Or just ask user? 
            // For now, let's look at the Root Cert Sig Algo.
            String sigAlgName = rootCert.getSigAlgName(); // e.g. "Dilithium5" or OID
            MlDsaLevel signingLevel = MlDsaLevel.ML_DSA_87; // Default
            
            if (sigAlgName.toUpperCase().contains("DILITHIUM2")) signingLevel = MlDsaLevel.ML_DSA_44;
            else if (sigAlgName.toUpperCase().contains("DILITHIUM3")) signingLevel = MlDsaLevel.ML_DSA_65;
            else if (sigAlgName.toUpperCase().contains("DILITHIUM5")) signingLevel = MlDsaLevel.ML_DSA_87;
            
            System.out.println("  Subject: " + subjectDn);
            System.out.println("  Signing Level: " + signingLevel);

            // 4. Generate Sub Cert
            X509Certificate subCert = pqc.generateSubordinateCertificate(
                    pubKey, 
                    subjectDn, 
                    keyPem, 
                    certPem, 
                    days, 
                    signingLevel, 
                    true // isCA = true for Sub CA
            );
            
            // 5. Write Output
            String subCertPem = pqc.certificateToPem(subCert);
            Files.writeString(outFile.toPath(), subCertPem);
            
            System.out.println("SUCCESS: Subordinate CA Certificate signed.");
            System.out.println("  - " + outFile.getAbsolutePath());
            
            return 0;
        }
    }
}
