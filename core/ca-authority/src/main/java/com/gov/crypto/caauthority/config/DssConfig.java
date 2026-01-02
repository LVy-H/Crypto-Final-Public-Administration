package com.gov.crypto.caauthority.config;

import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DssConfig {

    @Bean
    public CertificateVerifier certificateVerifier() {
        CommonCertificateVerifier verifier = new CommonCertificateVerifier();
        // Disabling advanced checking for now (AI, CRL, OCSP) to start simple
        // Will be enabled in later phases
        verifier.setCrlSource(null);
        verifier.setOcspSource(null);
        verifier.setTrustedCertSources(trustedCertSource());
        return verifier;
    }

    @Bean
    public CertificateSource trustedCertSource() {
        // CommonTrustedCertificateSource has type TRUSTED_STORE which is required
        return new CommonTrustedCertificateSource();
    }
}
