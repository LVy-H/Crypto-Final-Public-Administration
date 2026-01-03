package com.gov.crypto.common.config;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
public class MtlsConfig {

    private static final Logger log = LoggerFactory.getLogger(MtlsConfig.class);

    @PostConstruct
    public void registerBouncyCastle() {
        log.info("Registering Bouncy Castle Providers for PQC mTLS...");

        // 1. Register Bouncy Castle Provider (BC)
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("Registered BouncyCastleProvider");
        }

        // 2. Register Bouncy Castle JSSE Provider (BCJSSE) for TLS
        // We add it at position 2 (after BC?) or just add it.
        // For strict PQC TLS, it might need to be preferred.
        if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleJsseProvider());
            log.info("Registered BouncyCastleJsseProvider");
        }

        // Log registered providers for debugging
        // Arrays.stream(Security.getProviders()).forEach(p -> log.debug("Provider: {}",
        // p.getName()));
    }
}
