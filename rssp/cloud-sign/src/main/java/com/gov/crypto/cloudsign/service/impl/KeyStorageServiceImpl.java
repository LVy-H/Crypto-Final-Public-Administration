package com.gov.crypto.cloudsign.service.impl;

import com.gov.crypto.service.KeyStorageService;
import org.springframework.stereotype.Service;

@Service
public class KeyStorageServiceImpl implements KeyStorageService {

    @Override
    public String signHash(String keyAlias, String dataHashBase64, String algorithm) throws Exception {
        // TODO: Implement actual signing using stored keys
        return "mock-signature-base64";
    }

    @Override
    public String generateKeyPair(String alias, String algorithm) throws Exception {
        // TODO: Implement key pair generation and storage
        return "mock-public-key-base64";
    }

    @Override
    public String generateCsr(String alias, String subject) throws Exception {
        // TODO: Implement CSR generation
        return "mock-csr-pem";
    }
}
