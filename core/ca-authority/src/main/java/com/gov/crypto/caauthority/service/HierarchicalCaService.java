package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.CertificateAuthority.CaType;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.model.IssuedCertificate.CertStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade service for backward compatibility.
 * 
 * Delegates to:
 * - CaManagementService: CA hierarchy operations
 * - CertificateIssuanceService: Certificate lifecycle operations
 * 
 * @deprecated Use CaManagementService or CertificateIssuanceService directly
 */
@Service
public class HierarchicalCaService {

    private final CaManagementService caManagement;
    private final CertificateIssuanceService certIssuance;

    public HierarchicalCaService(CaManagementService caManagement,
            CertificateIssuanceService certIssuance) {
        this.caManagement = caManagement;
        this.certIssuance = certIssuance;
    }

    // ========== Delegate Records ==========

    public record CsrResult(String pendingCaId, String csrPem) {
        public static CsrResult from(CaManagementService.CsrResult r) {
            return new CsrResult(r.pendingCaId(), r.csrPem());
        }
    }

    public record ServiceCertificateResult(String certificate, String privateKey, String caCertificate) {
        public static ServiceCertificateResult from(CertificateIssuanceService.ServiceCertificateResult r) {
            return new ServiceCertificateResult(r.certificate(), r.privateKey(), r.caCertificate());
        }
    }

    // ========== CA Management Delegates ==========

    @Transactional
    public CsrResult generateCaCsr(String name, String algorithm) throws Exception {
        return CsrResult.from(caManagement.generateCaCsr(name, algorithm));
    }

    @Transactional
    public CertificateAuthority activateCaWithSignedCert(String pendingCaId,
            String certificatePem, String nationalRootCertPem) throws Exception {
        return caManagement.activateCaWithSignedCert(pendingCaId, certificatePem, nationalRootCertPem);
    }

    @Transactional
    public CertificateAuthority createSubordinate(UUID parentCaId, String name,
            CaType type, String algorithm, String label, int validDays) throws Exception {
        return caManagement.createSubordinate(parentCaId, name, type, algorithm, label, validDays);
    }

    @Transactional
    public CertificateAuthority registerExternalRa(UUID parentCaId, String name,
            String csrPem, String label, int validDays) throws Exception {
        return caManagement.registerExternalRa(parentCaId, name, csrPem, label, validDays);
    }

    @Transactional
    public CertificateAuthority createProvincialCa(UUID parentCaId, String provinceName) throws Exception {
        return caManagement.createProvincialCa(parentCaId, provinceName);
    }

    @Transactional
    public CertificateAuthority createDistrictRa(UUID parentCaId, String districtName) throws Exception {
        return caManagement.createDistrictRa(parentCaId, districtName);
    }

    @Transactional
    public void revokeCa(UUID caId, String reason) {
        caManagement.revokeCa(caId, reason);
    }

    public List<String> getCertificateChain(UUID caId) {
        return caManagement.getCertificateChain(caId);
    }

    public List<CertificateAuthority> getAllSubordinates(UUID caId) {
        return caManagement.getAllSubordinates(caId);
    }

    public List<CertificateAuthority> getCasByLevel(int hierarchyLevel) {
        return caManagement.getCasByLevel(hierarchyLevel);
    }

    public List<CertificateAuthority> getAllCas() {
        return caManagement.getAllCas();
    }

    public CertificateAuthority saveCa(CertificateAuthority ca) {
        return caManagement.saveCa(ca);
    }

    public Optional<CertificateAuthority> getCaById(UUID caId) {
        return caManagement.getCaById(caId);
    }

    public CertificateAuthority getCaByOrganizationId(UUID organizationId) {
        return caManagement.getCaByOrganizationId(organizationId);
    }

    public Optional<CertificateAuthority> getCaByLabel(String label) {
        return caManagement.getCaByLabel(label);
    }

    // ========== Certificate Issuance Delegates ==========

    @Transactional
    public ServiceCertificateResult issueServiceCertificate(String serviceName, List<String> dnsNames, int validDays)
            throws Exception {
        return ServiceCertificateResult.from(certIssuance.issueServiceCertificate(serviceName, dnsNames, validDays));
    }

    @Transactional
    public IssuedCertificate issueUserCertificate(UUID issuingRaId, String csrContent, String subjectDn)
            throws Exception {
        return certIssuance.issueUserCertificate(issuingRaId, csrContent, subjectDn);
    }

    @Transactional
    public IssuedCertificate createCertificateRequest(String username, String algorithm, String csrPem) {
        return certIssuance.createCertificateRequest(username, algorithm, csrPem);
    }

    @Transactional
    public IssuedCertificate approveCertificate(UUID requestId) throws Exception {
        return certIssuance.approveCertificate(requestId);
    }

    @Transactional
    public void revokeCertificate(UUID certId, String reason) {
        certIssuance.revokeCertificate(certId, reason);
    }

    @Transactional
    public String generateCrl(UUID caId) throws Exception {
        return certIssuance.generateCrl(caId);
    }

    public List<IssuedCertificate> getUserCertificates(String username) {
        return certIssuance.getUserCertificates(username);
    }

    public List<IssuedCertificate> getAllIssuedCertificates() {
        return certIssuance.getAllIssuedCertificates();
    }

    public List<IssuedCertificate> getCertificatesByStatus(CertStatus status) {
        return certIssuance.getCertificatesByStatus(status);
    }

    public Map<String, Long> getCertificateStats() {
        return certIssuance.getCertificateStats();
    }
}
