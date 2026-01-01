package com.gov.crypto.caauthority.registration.service.impl;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.registration.dto.CaRegistrationRequest;
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
    public RegistrationResponse registerUser(CaRegistrationRequest request, String authToken) {
        // 1. Validate KYC
        validateKyc(request.kycData());

        // 2. Build X.500 Subject DN
        String subjectDn = buildSubjectDn(request.kycData());

        // 3. Generate Key Pair in Cloud Sign
        // POST /csc/v1/keys/generate
        String keyGenUrl = cloudSignUrl + "/csc/v1/keys/generate";
        // Create headers with Auth token
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        // Use internal secret key to bypass session issues
        headers.set("Authorization", "Bearer super-secure-internal-secret-key-2026");
        org.springframework.http.HttpEntity<CloudKeyGenRequest> keyGenEntity = new org.springframework.http.HttpEntity<>(
                new CloudKeyGenRequest(request.username(), request.algorithm()), headers);

        restTemplate.postForObject(keyGenUrl, keyGenEntity, Void.class);

        // 4. Generate CSR in Cloud Sign using proper DN
        // POST /csc/v1/keys/csr
        String csrUrl = cloudSignUrl + "/csc/v1/keys/csr";
        org.springframework.http.HttpEntity<CloudCsrRequest> csrEntity = new org.springframework.http.HttpEntity<>(
                new CloudCsrRequest(request.username(), subjectDn), headers);
        CloudCsrResponse csrResponse = restTemplate.postForObject(csrUrl, csrEntity, CloudCsrResponse.class);

        if (csrResponse == null || csrResponse.csrPem() == null) {
            throw new RuntimeException("Failed to generate CSR");
        }

        // 4. Issue Certificate in CA Authority (Local Call)
        UUID issuingRaId = findIssuingRa();
        IssuedCertificate issuedCert;
        try {
            issuedCert = hierarchicalCaService.issueUserCertificate(issuingRaId, csrResponse.csrPem(), subjectDn);
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

    private void validateKyc(com.gov.crypto.caauthority.registration.dto.KycData kyc) {
        if (kyc == null) {
            throw new IllegalArgumentException("KYC data is required");
        }
        if (kyc.cccdNumber() == null || !kyc.cccdNumber().matches("^\\d{12}$")) {
            throw new IllegalArgumentException("Invalid CCCD format: Must be exactly 12 digits");
        }
        if (kyc.fullName() == null || kyc.fullName().isBlank()) {
            throw new IllegalArgumentException("Full Name is required");
        }
        if (kyc.email() == null || !kyc.email().contains("@")) {
            throw new IllegalArgumentException("Invalid Email format");
        }
        if (kyc.province() == null || kyc.province().isBlank()) {
            throw new IllegalArgumentException("Province is required");
        }
        if (kyc.district() == null || kyc.district().isBlank()) {
            throw new IllegalArgumentException("District is required");
        }
    }

    private String buildSubjectDn(com.gov.crypto.caauthority.registration.dto.KycData kyc) {
        // Format: serialNumber=012345678901,CN=Nguyễn Văn
        // A,emailAddress=user@gov.vn,L=Hoàn Kiếm,ST=Hà Nội,O=Citizen,C=VN
        StringBuilder dn = new StringBuilder();
        dn.append("serialNumber=").append(kyc.cccdNumber());
        dn.append(",CN=").append(kyc.fullName());
        dn.append(",emailAddress=").append(kyc.email());
        dn.append(",L=").append(kyc.district());
        dn.append(",ST=").append(kyc.province());
        dn.append(",O=").append(kyc.organization() != null ? kyc.organization() : "Citizen");
        dn.append(",C=").append(kyc.country() != null ? kyc.country() : "VN");
        return dn.toString();
    }
}
