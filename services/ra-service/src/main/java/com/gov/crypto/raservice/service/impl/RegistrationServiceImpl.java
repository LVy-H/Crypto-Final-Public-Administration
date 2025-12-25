package com.gov.crypto.raservice.service.impl;

import com.gov.crypto.raservice.dto.RegistrationRequest;
import com.gov.crypto.raservice.dto.RegistrationResponse;
import com.gov.crypto.raservice.service.RegistrationService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RestTemplate restTemplate;

    @Value("${service.cloud-sign.url:http://cloud-sign:8084}")
    private String cloudSignUrl;

    @Value("${service.ca-authority.url:http://ca-authority:8082}")
    private String caAuthorityUrl;

    public RegistrationServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    record CloudKeyGenRequest(String alias, String algorithm) {
    }

    record CloudCsrRequest(String alias, String subject) {
    }

    record CloudCsrResponse(String csrPem) {
    }

    record CaIssueRequest(String csrPem) {
    }

    record CaCertResponse(String certificatePem) {
    }

    @Override
    public RegistrationResponse registerUser(RegistrationRequest request) {
        // 1. Validate KYC (Mock)
        if (request.username() == null || request.email() == null) {
            throw new IllegalArgumentException("Invalid Request");
        }

        // 2. Generate Key Pair in Cloud Sign
        // POST /api/v1/cloud-sign/keys/generate
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

        // 4. Issue Certificate in CA Authority
        // POST /api/v1/ca/issue
        String caUrl = caAuthorityUrl + "/api/v1/ca/issue";
        CaCertResponse certResponse = restTemplate.postForObject(caUrl, new CaIssueRequest(csrResponse.csrPem()),
                CaCertResponse.class);

        if (certResponse == null || certResponse.certificatePem() == null) {
            throw new RuntimeException("Failed to issue Certificate");
        }

        // 5. Return Response
        return new RegistrationResponse(request.username(), "REGISTERED", certResponse.certificatePem());
    }
}
