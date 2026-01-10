package com.gov.crypto.offlineca;

import com.gov.crypto.common.service.PqcCryptoService;
import com.gov.crypto.common.service.PqcAlgorithm;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

@Command(name = "offline-ca", mixinStandardHelpOptions = true, version = "1.1", description = "Offline Root CA Management Tool for PQC (ML-DSA, SLH-DSA)", subcommands = {
        OfflineCaTool.InitRootCommand.class, OfflineCaTool.SignCsrCommand.class,
        OfflineCaTool.GenerateCsrCommand.class })
public class OfflineCaTool implements Callable<Integer> {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider());
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new OfflineCaTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "init-root", description = "Initialize a new PQC Root CA")
    static class InitRootCommand implements Callable<Integer> {

        @Option(names = { "-n",
                "--name" }, description = "Subject DN (default: CN=National Root CA, O=Government)", defaultValue = "CN=National Root CA, O=Government")
        private String name;

        @Option(names = { "-a",
                "--algo" }, description = "Algorithm: ML_DSA_44, ML_DSA_65, ML_DSA_87, SLH_DSA_SHAKE_128F (default: ML_DSA_87)", defaultValue = "ML_DSA_87")
        private PqcAlgorithm algo;

        @Option(names = { "-d",
                "--days" }, description = "Validity days (default: 7300 - 20 years)", defaultValue = "7300")
        private int days;

        @Option(names = { "-o", "--out-dir" }, description = "Output directory", required = true)
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
            AsymmetricCipherKeyPair keyPair = pqc.generateKeyPair(algo);

            // 2. Self-Sign Cert
            X509Certificate cert = pqc.generateSelfSignedCertificate(keyPair, name, days, algo);

            // 3. Save Private Key
            String privKeyPem = pqc.privateKeyToPem(keyPair.getPrivate());
            Files.writeString(Path.of(outDir.getAbsolutePath(), "root.key"), privKeyPem);

            // 4. Save Public Key
            String pubKeyPem = pqc.publicKeyToPem(keyPair.getPublic());
            Files.writeString(Path.of(outDir.getAbsolutePath(), "root.pub"), pubKeyPem);

            // 5. Save Cert
            String certPem = pqc.certificateToPem(cert);
            Files.writeString(Path.of(outDir.getAbsolutePath(), "root.crt"), certPem);

            System.out.println("SUCCESS: Root CA initialized.");
            return 0;
        }
    }

    @Command(name = "sign-csr", description = "Sign a Subordinate CA CSR")
    static class SignCsrCommand implements Callable<Integer> {

        @Option(names = { "--csr" }, description = "Path to CSR file", required = true)
        private File csrFile;

        @Option(names = { "--key" }, description = "Path to Root CA Private Key", required = true)
        private File keyFile;

        @Option(names = { "--cert" }, description = "Path to Root CA Certificate", required = true)
        private File certFile;

        @Option(names = { "--out" }, description = "Output path for signed certificate", required = true)
        private File outFile;

        @Option(names = { "-d",
                "--days" }, description = "Validity days (default: 1825 - 5 years)", defaultValue = "1825")
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
            AsymmetricKeyParameter rootKey = pqc.parsePrivateKeyPem(keyPem);
            X509Certificate rootCert = pqc.parseCertificatePem(certPem);

            // 3. Parse CSR
            PKCS10CertificationRequest csr = pqc.parseCsrPem(csrPem);

            // Determine Algo from Root Cert Sig Algo or user input?
            // Existing logic checked generic names. Let's do similar or better.
            String sigAlgName = rootCert.getSigAlgName().toUpperCase();
            PqcAlgorithm signingAlgo = PqcAlgorithm.ML_DSA_87; // fallback

            if (sigAlgName.contains("DILITHIUM2") || sigAlgName.contains("ML-DSA-44"))
                signingAlgo = PqcAlgorithm.ML_DSA_44;
            else if (sigAlgName.contains("DILITHIUM3") || sigAlgName.contains("ML-DSA-65"))
                signingAlgo = PqcAlgorithm.ML_DSA_65;
            else if (sigAlgName.contains("DILITHIUM5") || sigAlgName.contains("ML-DSA-87"))
                signingAlgo = PqcAlgorithm.ML_DSA_87;
            else if (sigAlgName.contains("SPHINCS") || sigAlgName.contains("SLH"))
                signingAlgo = PqcAlgorithm.SLH_DSA_SHAKE_128F;

            System.out.println("  Signing with Algo: " + signingAlgo);

            // 4. Generate Sub Cert
            X509Certificate subCert = pqc.signCsr(
                    csr,
                    rootKey,
                    rootCert,
                    days,
                    signingAlgo,
                    true // isCa = true
            );

            // 5. Write Output
            String subCertPem = pqc.certificateToPem(subCert);
            Files.writeString(outFile.toPath(), subCertPem);

            System.out.println("SUCCESS: Subordinate CA Certificate signed.");
            return 0;
        }
    }

    @Command(name = "gen-infra-csr", description = "Generate Infrastructure Intermediate CA Key & CSR")
    static class GenerateCsrCommand implements Callable<Integer> {

        @Option(names = { "-n",
                "--name" }, description = "Subject DN (default: CN=Infrastructure CA, O=Government)", defaultValue = "CN=Infrastructure CA, O=Government")
        private String name;

        @Option(names = { "-a",
                "--algo" }, description = "Algorithm: ML_DSA_44, ML_DSA_65, ML_DSA_87, SLH_DSA_SHAKE_128F (default: ML_DSA_65)", defaultValue = "ML_DSA_65")
        private PqcAlgorithm algo;

        @Option(names = { "-o", "--out-dir" }, description = "Output directory", required = true)
        private File outDir;

        @Override
        public Integer call() throws Exception {
            System.out.println("Generating Infrastructure CA CSR...");

            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("Failed to create output directory");
                return 1;
            }

            PqcCryptoService pqc = new PqcCryptoService();

            // 1. Generate Key Pair
            AsymmetricCipherKeyPair keyPair = pqc.generateKeyPair(algo);

            // 2. Generate CSR
            PKCS10CertificationRequest csr = pqc.generateCsr(keyPair, name, algo);

            // 3. Save Private Key
            String privKeyPem = pqc.privateKeyToPem(keyPair.getPrivate());
            Files.writeString(Path.of(outDir.getAbsolutePath(), "infra.key"), privKeyPem);

            // 4. Save CSR
            String csrPem = pqc.csrToPem(csr);
            Files.writeString(Path.of(outDir.getAbsolutePath(), "infra.csr"), csrPem);

            System.out.println("SUCCESS: Infra CA Key & CSR generated.");
            return 0;
        }
    }
}
