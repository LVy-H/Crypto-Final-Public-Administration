package com.gov.crypto.common.tool;

import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.common.pqc.PqcCryptoService.MlDsaLevel;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pqc-bootstrap", mixinStandardHelpOptions = true, description = "Bootstrap mTLS Certificates for K8s Pods")
public class PqcBootstrap implements Callable<Integer> {

    @Option(names = { "--ca-key" }, description = "Path to Infra CA Private Key", required = true)
    private File caKeyFile;

    @Option(names = { "--ca-cert" }, description = "Path to Infra CA Certificate", required = true)
    private File caCertFile;

    @Option(names = { "--out-dir" }, description = "Output directory for generated certs", required = true)
    private File outDir;

    @Option(names = { "--dns" }, description = "DNS Name (SAN) for the service", required = true)
    private String dnsName;

    @Option(names = { "--ip" }, description = "IP Address (SAN) for the service")
    private String ipAddress;

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastlePQCProvider());
        int exitCode = new CommandLine(new PqcBootstrap()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Bootstrapping mTLS Certificate...");
        System.out.println("  DNS: " + dnsName);
        System.out.println("  Output: " + outDir.getAbsolutePath());

        PqcCryptoService pqc = new PqcCryptoService();

        // 1. Generate Ephemeral Key Pair (ML-DSA-44 for performance)
        System.out.println("Generating KeyPair (ML-DSA-44)...");
        KeyPair keyPair = pqc.generateMlDsaKeyPair(MlDsaLevel.ML_DSA_44);

        // 2. Read CA Material
        String caKeyPem = Files.readString(caKeyFile.toPath());
        String caCertPem = Files.readString(caCertFile.toPath());

        // 3. Generate Certificate Signed by Infra CA
        // Subject DN: CN=<dnsName>, O=Gov Internal
        String subjectDn = "CN=" + dnsName + ", O=Gov Internal";
        System.out.println("Signing Certificate with Infra CA...");

        // Use PqcCryptoService to generate subordinate cert (acts as leaf here, but
        // same logic)
        // We set isCA=false for leaf
        X509Certificate cert = pqc.generateSubordinateCertificate(
                keyPair.getPublic(),
                subjectDn,
                caKeyPem,
                caCertPem,
                1, // 1 day validity (short-lived)
                MlDsaLevel.ML_DSA_65, // Infra CA is ML-DSA-65
                false // isCA = false (Leaf)
        );

        // 4. Write Output
        if (!outDir.exists() && !outDir.mkdirs()) {
            System.err.println("Failed to create output dir");
            return 1;
        }

        Files.writeString(Path.of(outDir.getAbsolutePath(), "service.pem"), pqc.certificateToPem(cert));
        Files.writeString(Path.of(outDir.getAbsolutePath(), "service-key.pem"),
                pqc.privateKeyToPem(keyPair.getPrivate()));
        Files.writeString(Path.of(outDir.getAbsolutePath(), "internal-ca.pem"), caCertPem); // Trust anchor

        System.out.println("SUCCESS: mTLS Certificate Bootstrap complete.");
        return 0;
    }
}
