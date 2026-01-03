package com.gov.crypto.doc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Client for calling identity-service APIs.
 * Used for officer assignment queries.
 */
@Service
public class IdentityServiceClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityServiceClient.class);

    private final WebClient webClient;

    public IdentityServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.identity.url:http://identity-service:8081}") String identityServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(identityServiceUrl)
                .build();
    }

    /**
     * Get officers assigned to a specific CA/RA.
     * Used for auto-assignment of countersigners.
     */
    public List<OfficerInfo> getOfficersForCa(UUID caId) {
        try {
            log.debug("Fetching officers for CA: {}", caId);

            List<OfficerInfo> officers = webClient.get()
                    .uri("/api/v1/officers/by-ca/{caId}", caId)
                    .retrieve()
                    .bodyToFlux(OfficerInfo.class)
                    .collectList()
                    .block();

            log.debug("Found {} officers for CA {}",
                    officers != null ? officers.size() : 0, caId);
            return officers != null ? officers : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to fetch officers for CA {}: {}", caId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * DTO for officer information from identity-service.
     */
    public record OfficerInfo(UUID id, String username, String caType) {
    }
}
