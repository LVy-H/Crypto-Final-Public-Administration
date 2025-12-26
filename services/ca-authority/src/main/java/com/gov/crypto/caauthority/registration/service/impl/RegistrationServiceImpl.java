package com.gov.crypto.caauthority.registration.service.impl;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.registration.dto.RegistrationRequest;
import com.gov.crypto.caauthority.registration.dto.RegistrationResponse;
import com.gov.crypto.caauthority.registration.service.RegistrationService;
import com.gov.crypto.caauthority.service.HierarchicalCaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RestTemplate restTemplate;
    private final HierarchicalCaService hierarchicalCaService;

    @Value("${service.cloud-sign.url:http://cloud-sign:8084}")
    private String cloudSignUrl;

    public RegistrationServiceImpl(RestTemplate restTemplate, HierarchicalCaService hierarchicalCaService) {
        this.restTemplate = restTemplate;
        this.hierarchicalCaService = hierarchicalCaService;
    }

    record CloudKeyGenRequest(String alias, String algorithm) {
    }

    record CloudCsrRequest(String alias, String subject) {
    }

    record CloudCsrResponse(String csrPem) {
    }

    @Override
    public RegistrationResponse registerUser(RegistrationRequest request) {
        // 1. Validate KYC (Mock)
        if (request.username() == null || request.email() == null) {
            throw new IllegalArgumentException("Invalid Request");
        }

        // 2. Generate Key Pair in Cloud Sign
        // POST /api/v1/cloud-sign/keys/generate
        // Note: Using username as alias for simplicity, in prod should use unique ID
        String keyGenUrl = cloudSignUrl + "/api/v1/cloud-sign/keys/generate";
        restTemplate.postForObject(keyGenUrl, new CloudKeyGenRequest(request.username(), request.algorithm()),
                Void.class);

        // 3. Generate CSR in Cloud Sign
        // POST /api/v1/cloud-sign/keys/csr
        String csrUrl = cloudSignUrl + "/api/v1/cloud-sign/keys/csr";
        String subject = "/CN=" + request.username() + "/emailAddress=" + request.email();
        CloudCsrResponse csrResponse = restTemplate.postForObject(csrUrl,
                new CloudCsrRequest(request.username(), subject), CloudCsrResponse.class);

        if (csrResponse == null || csrResponse.csrPem() == null) {
            throw new RuntimeException("Failed to generate CSR");
        }

        // 4. Issue Certificate in CA Authority (Local Call)
        UUID issuingRaId = findIssuingRa();
        IssuedCertificate issuedCert;
        try {
            issuedCert = hierarchicalCaService.issueUserCertificate(issuingRaId, csrResponse.csrPem(), subject);
        } catch (Exception e) {
            throw new RuntimeException("Failed to issue Certificate: " + e.getMessage(), e);
        }

        // 5. Return Response
        return new RegistrationResponse(request.username(), "REGISTERED", issuedCert.getCertificate());
    }

    private UUID findIssuingRa() {
        // Find a District RA to issue the certificate
        List<CertificateAuthority> ras = hierarchicalCaService.getCasByLevel(CertificateAuthority.CaLevel.DISTRICT);
        if (ras.isEmpty()) {
            // Fallback: Check for Provincial if no District
            List<CertificateAuthority> provCas = hierarchicalCaService
                    .getCasByLevel(CertificateAuthority.CaLevel.PROVINCIAL);
            if (!provCas.isEmpty()) {
                return provCas.get(0).getId();
            }
            // Fallback: Check for Root if nothing else (e.g. strict dev env)
            List<CertificateAuthority> rootCas = hierarchicalCaService.getCasByLevel(CertificateAuthority.CaLevel.ROOT);
            if (!rootCas.isEmpty()) {
                return rootCas.get(0).getId();
            }
            throw new RuntimeException("No active CA/RA found to issue user certificate.");
        }
        // Naive selection: pick the first one. In future, select based on user
        // location/assignments.
        return ras.get(0).getId();
    }
}
