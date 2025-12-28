package com.gov.crypto.caauthority.util;

import com.gov.crypto.common.pqc.PqcCryptoService;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientCsrGeneratorTest {

    @Test
    public void generateTestCsr() throws Exception {
        PqcCryptoService pqcService = new PqcCryptoService();

        System.out.println("Generating Key Pair (ML-DSA-44)...");
        KeyPair keyPair = pqcService.generateMlDsaKeyPair(PqcCryptoService.MlDsaLevel.ML_DSA_44);

        // Save keys
        String privateKeyPem = pqcService.privateKeyToPem(keyPair.getPrivate());
        String publicKeyPem = pqcService.publicKeyToPem(keyPair.getPublic());

        Files.writeString(Path.of("test_user_private.key"), privateKeyPem);
        Files.writeString(Path.of("test_user_public.pem"), publicKeyPem);

        System.out.println("Generating CSR for 'test_browser_user'...");
        PKCS10CertificationRequest csr = pqcService.generateCsr(
                keyPair,
                "CN=test_browser_user, O=Citizen, C=VN",
                PqcCryptoService.MlDsaLevel.ML_DSA_44);

        String csrPem = pqcService.csrToPem(csr);
        Files.writeString(Path.of("test_user.csr"), csrPem);

        System.out.println("CSR Generated:");
        System.out.println(csrPem);
    }
}
