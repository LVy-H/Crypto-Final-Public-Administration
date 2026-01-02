package com.gov.crypto.common.validation;

import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import org.springframework.stereotype.Component;

/**
 * Wrapper for SD-DSS CommonCertificateVerifier to be used to be managed by
 * Spring.
 * Ensures consistent configuration across the application.
 */
@Component
public class DssCertificateVerifier extends CommonCertificateVerifier {

    public DssCertificateVerifier() {
        super();

        // Configure Data Loader for fetching CRL/OCSP
        CommonsDataLoader dataLoader = new CommonsDataLoader();

        // Enable Online CRL Checking
        OnlineCRLSource crlSource = new OnlineCRLSource();
        crlSource.setDataLoader(dataLoader);
        setCrlSource(crlSource);

        // Enable Online OCSP Checking
        OnlineOCSPSource ocspSource = new OnlineOCSPSource();
        ocspSource.setDataLoader(dataLoader);
        setOcspSource(ocspSource);

        // Enable revocation checking
        setCheckRevocationForUntrustedChains(true);
    }
}
