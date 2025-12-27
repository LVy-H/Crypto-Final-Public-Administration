package com.gov.crypto.caauthority.repository;

import com.gov.crypto.caauthority.model.CertificateAuthority;
import com.gov.crypto.caauthority.model.IssuedCertificate;
import com.gov.crypto.caauthority.model.IssuedCertificate.CertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssuedCertificateRepository extends JpaRepository<IssuedCertificate, UUID> {

    Optional<IssuedCertificate> findBySerialNumber(String serialNumber);

    List<IssuedCertificate> findByIssuingCa(CertificateAuthority issuingCa);

    List<IssuedCertificate> findByStatus(CertStatus status);

    List<IssuedCertificate> findBySubjectDnContaining(String subjectPart);
}
