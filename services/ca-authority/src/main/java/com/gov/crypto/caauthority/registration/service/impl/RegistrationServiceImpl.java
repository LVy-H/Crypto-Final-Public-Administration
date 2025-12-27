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
    public RegistrationResponse registerUser(RegistrationRequest request, String authToken) {
        // 1. Validate KYC (Mock)
        if (request.username() == null || request.email() == null) {
            throw new IllegalArgumentException("Invalid Request");
        }

        // 2. Generate Key Pair in Cloud Sign
        // POST /api/v1/cloud-sign/keys/generate
        // Note: Using username as alias for simplicity, in prod should use unique ID
        String keyGenUrl = cloudSignUrl + "/api/v1/cloud-sign/keys/generate";
        // Create headers with Auth token
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", authToken);
        org.springframework.http.HttpEntity<CloudKeyGenRequest> keyGenEntity = new org.springframework.http.HttpEntity<>(
                new CloudKeyGenRequest(request.username(), request.algorithm()), headers);

        restTemplate.postForObject(keyGenUrl, keyGenEntity, Void.class);

        // 3. Generate CSR in Cloud Sign
        // POST /api/v1/cloud-sign/keys/csr
        String csrUrl = cloudSignUrl + "/api/v1/cloud-sign/keys/csr";
        String subject = "/CN=" + request.username() + "/emailAddress=" + request.email();
        org.springframework.http.HttpEntity<CloudCsrRequest> csrEntity = new org.springframework.http.HttpEntity<>(
                new CloudCsrRequest(request.username(), subject), headers);
        CloudCsrResponse csrResponse = restTemplate.postForObject(csrUrl, csrEntity, CloudCsrResponse.class);

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
        // Find a District RA to issue the certificate (Level 2)
        List<CertificateAuthority> ras = hierarchicalCaService.getCasByLevel(2);
        if (ras.isEmpty()) {
            // Fallback: Check for Provincial if no District (Level 1)
            List<CertificateAuthority> provCas = hierarchicalCaService.getCasByLevel(1);
            if (!provCas.isEmpty()) {
                return provCas.get(0).getId();
            }
            // Fallback: Check for Root if nothing else (Level 0)
            List<CertificateAuthority> rootCas = hierarchicalCaService.getCasByLevel(0);
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
