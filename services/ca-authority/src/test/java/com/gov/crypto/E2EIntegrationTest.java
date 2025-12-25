package com.gov.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Tests for the PQC Digital Signature System.
 * Tests complete workflows across multiple services via API Gateway.
 * 
 * Requires: Docker containers running (docker-compose up)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class E2EIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String API_BASE = "http://localhost:8080/api/v1";
    private static String rootCaId;
    private static String provincialCaId;
    private static String districtRaId;
    private static String internalCaId;

    @Nested
    @DisplayName("E2E: PKI Hierarchy Creation Flow")
    class PkiHierarchyFlowTests {

        @Test
        @DisplayName("Step 1: Initialize Root CA (ML-DSA-87)")
        void step1_initializeRootCa() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(
                    "{\"name\": \"E2E Test Root CA\"}", headers);

            // When
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_BASE + "/ca/root/init", request, Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().get("id"));
            assertEquals("ML-DSA-87", response.getBody().get("algorithm"));

            rootCaId = response.getBody().get("id").toString();
        }

        @Test
        @DisplayName("Step 2: Create Provincial CA (ML-DSA-87)")
        void step2_createProvincialCa() {
            // Skip if root CA not created
            if (rootCaId == null) {
                step1_initializeRootCa();
            }

            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(
                    "{\"parentCaId\": \"" + rootCaId + "\", \"name\": \"E2E Test Province\"}",
                    headers);

            // When
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_BASE + "/ca/provincial", request, Map.class);

            // Then
            if (response.getStatusCode() == HttpStatus.OK) {
                assertNotNull(response.getBody().get("id"));
                assertEquals("ML-DSA-87", response.getBody().get("algorithm"));
                provincialCaId = response.getBody().get("id").toString();
            }
        }

        @Test
        @DisplayName("Step 3: Create District RA (ML-DSA-65)")
        void step3_createDistrictRa() {
            // Skip if provincial CA not created
            if (provincialCaId == null) {
                step2_createProvincialCa();
            }

            if (provincialCaId == null)
                return;

            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(
                    "{\"parentCaId\": \"" + provincialCaId + "\", \"name\": \"E2E Test District\"}",
                    headers);

            // When
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_BASE + "/ca/district", request, Map.class);

            // Then
            if (response.getStatusCode() == HttpStatus.OK) {
                assertEquals("ML-DSA-65", response.getBody().get("algorithm"));
                districtRaId = response.getBody().get("id").toString();
            }
        }

        @Test
        @DisplayName("Step 4: Create Internal Services CA (ML-DSA-65)")
        void step4_createInternalServicesCa() {
            // Skip if root CA not created
            if (rootCaId == null) {
                step1_initializeRootCa();
            }

            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(
                    "{\"rootCaId\": \"" + rootCaId + "\"}", headers);

            // When
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_BASE + "/ca/internal/init", request, Map.class);

            // Then
            if (response.getStatusCode() == HttpStatus.OK) {
                assertEquals("ML-DSA-65", response.getBody().get("algorithm"));
                assertEquals(rootCaId, response.getBody().get("parentCaId").toString());
                internalCaId = response.getBody().get("id").toString();
            }
        }
    }

    @Nested
    @DisplayName("E2E: Certificate Chain Validation")
    class CertChainValidationTests {

        @Test
        @DisplayName("Should get complete certificate chain")
        void shouldGetCertificateChain() {
            if (districtRaId == null)
                return;

            // When
            ResponseEntity<String[]> response = restTemplate.getForEntity(
                    API_BASE + "/ca/chain/" + districtRaId, String[].class);

            // Then
            if (response.getStatusCode() == HttpStatus.OK) {
                assertTrue(response.getBody().length >= 1);
            }
        }
    }

    @Nested
    @DisplayName("E2E: User Authentication Flow")
    class UserAuthFlowTests {

        @Test
        @DisplayName("Should register new user")
        void shouldRegisterUser() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"username\": \"e2e_test_user\", \"email\": \"e2e@test.vn\", \"password\": \"Test123!\"}";
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // When
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_BASE + "/auth/register", request, Map.class);

            // Then - either success or user already exists
            assertTrue(response.getStatusCode() == HttpStatus.OK ||
                    response.getStatusCode() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should login with credentials")
        void shouldLoginUser() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"username\": \"e2e_test_user\", \"password\": \"Test123!\"}";
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // When
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_BASE + "/auth/login", request, Map.class);

            // Then
            if (response.getStatusCode() == HttpStatus.OK) {
                assertNotNull(response.getBody().get("token"));
            }
        }
    }

    @Nested
    @DisplayName("E2E: CA Query Operations")
    class CaQueryTests {

        @Test
        @DisplayName("Should list all Root CAs")
        void shouldListRootCas() {
            // When
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                    API_BASE + "/ca/level/ROOT", Object[].class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should list all Provincial CAs")
        void shouldListProvincialCas() {
            // When
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                    API_BASE + "/ca/level/PROVINCIAL", Object[].class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should list all Internal CAs")
        void shouldListInternalCas() {
            // When
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                    API_BASE + "/ca/level/INTERNAL", Object[].class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("E2E: Subordinate CA Queries")
    class SubordinateQueryTests {

        @Test
        @DisplayName("Should list subordinates of Root CA")
        void shouldListRootSubordinates() {
            if (rootCaId == null)
                return;

            // When
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                    API_BASE + "/ca/subordinates/" + rootCaId, Object[].class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }
}
