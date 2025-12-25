package com.gov.crypto.cloudsign.controller;

import com.gov.crypto.cloudsign.service.OpenSSLService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/csc/v1")
public class SigningController {

    private final OpenSSLService openSSLService;

    public SigningController(OpenSSLService openSSLService) {
        this.openSSLService = openSSLService;
    }

    record SignRequest(String keyAlias, String dataHashBase64, String algorithm) {
    }

    record SignResponse(String signatureBase64) {
    }

    record KeyGenRequest(String alias, String algorithm) {
    }

    record KeyGenResponse(String publicKeyPem) {
    }

    @PostMapping("/sign")
    public ResponseEntity<SignResponse> signHash(@RequestBody SignRequest request) {
        try {
            // In a real CSC implementation, we would validate the SAD (Authorization) here.
            String signature = openSSLService.signHash(request.keyAlias(), request.dataHashBase64(),
                    request.algorithm());
            return ResponseEntity.ok(new SignResponse(signature));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/keys/generate")
    public ResponseEntity<KeyGenResponse> generateKey(@RequestBody KeyGenRequest request) {
        try {
            String publicKey = openSSLService.generateKeyPair(request.alias(), request.algorithm());
            return ResponseEntity.ok(new KeyGenResponse(publicKey));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    record CsrRequest(String alias, String subject) {
    }

    record CsrResponse(String csrPem) {
    }

    @PostMapping("/keys/csr")
    public ResponseEntity<CsrResponse> generateCsr(@RequestBody CsrRequest request) {
        try {
            String csr = openSSLService.generateCsr(request.alias(), request.subject());
            return ResponseEntity.ok(new CsrResponse(csr));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
