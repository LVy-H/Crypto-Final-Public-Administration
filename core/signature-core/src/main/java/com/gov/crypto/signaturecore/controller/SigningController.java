package com.gov.crypto.signaturecore.controller;

import com.gov.crypto.common.pqc.PqcCryptoService;
import com.gov.crypto.signaturecore.dto.SignRequest;
import com.gov.crypto.signaturecore.dto.SignResponse;
import org.springframework.web.bind.annotation.*;
import java.security.PrivateKey;
import java.security.KeyPair;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/sign")
public class SigningController {

    private final PqcCryptoService pqcCryptoService;

    public SigningController(PqcCryptoService pqcCryptoService) {
        this.pqcCryptoService = pqcCryptoService;
    }

    @PostMapping("/remote")
    public SignResponse signRemote(@RequestBody SignRequest request) {
        try {
            // Generates ephemeral key pair for demo/verification
            // In prod: Retrieve persistent key from HSM/Vault using keyAlias
            KeyPair keyPair = pqcCryptoService.generateMlDsaKeyPair(PqcCryptoService.MlDsaLevel.ML_DSA_44);
            PrivateKey privateKey = keyPair.getPrivate();

            byte[] dataToSign = Base64.getDecoder().decode(request.dataBase64());
            byte[] signature = pqcCryptoService.sign(dataToSign, privateKey, PqcCryptoService.MlDsaLevel.ML_DSA_44);

            return new SignResponse(Base64.getEncoder().encodeToString(signature), "ML-DSA-44");
        } catch (Exception e) {
            throw new RuntimeException("Signing failed: " + e.getMessage(), e);
        }
    }
}
