package com.gov.crypto.caauthority.repository;

import com.gov.crypto.caauthority.model.CaPendingRequest;
import com.gov.crypto.caauthority.model.CaPendingRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaPendingRequestRepository extends JpaRepository<CaPendingRequest, UUID> {
    List<CaPendingRequest> findByStatus(RequestStatus status);

    List<CaPendingRequest> findByRequestedBy(String requestedBy);
}
