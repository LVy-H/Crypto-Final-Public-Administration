package com.gov.crypto.caauthority.config;

import com.gov.crypto.common.pqc.PqcCryptoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for PQC cryptographic services.
 */
@Configuration
public class PqcCryptoConfig {

    @Bean
    public PqcCryptoService pqcCryptoService() {
        return new PqcCryptoService();
    }
}
