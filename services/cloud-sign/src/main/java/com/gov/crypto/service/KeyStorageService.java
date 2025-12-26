package com.gov.crypto.service;

public interface KeyStorageService {
    String signHash(String keyAlias, String dataHashBase64, String algorithm) throws Exception;

    String generateKeyPair(String alias, String algorithm) throws Exception;

    String generateCsr(String alias, String subject) throws Exception;
}
