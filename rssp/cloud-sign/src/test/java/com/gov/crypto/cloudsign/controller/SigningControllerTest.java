package com.gov.crypto.cloudsign.controller;

import com.gov.crypto.cloudsign.service.SigningChallengeService;
import com.gov.crypto.cloudsign.service.SigningChallengeService.ChallengeCreatedResult;
import com.gov.crypto.cloudsign.service.SigningChallengeService.SigningChallenge;
import com.gov.crypto.cloudsign.service.SigningChallengeService.VerificationResult;
import com.gov.crypto.cloudsign.security.SadValidator;
import com.gov.crypto.cloudsign.security.SadValidator.ValidationResult;
import com.gov.crypto.cloudsign.controller.SigningController.*;
import com.gov.crypto.common.tsa.TsaClient;
import com.gov.crypto.common.tsa.TsaClient.TimestampResult;
import com.gov.crypto.service.KeyStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.Instant;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SigningControllerTest {

    @Mock
    private KeyStorageService keyStorageService;

    @Mock
    private SadValidator sadValidator;

    @Mock
    private SigningChallengeService signingChallengeService;

    @Mock
    private TsaClient tsaClient;

    @Mock
    private Principal principal;

    private SigningController signingController;

    private final String TEST_USER = "testUser";
    private final String KEY_ALIAS = "testUser_key1";
    private final String DOC_HASH = "deadbeef";
    private final String ALGO = "ML-DSA-65";
    private final String CHALLENGE_ID = "challenge-123";
    private final String OTP = "123456";
    private final String SIGNATURE = "SGVsbG8gV29ybGQ="; // "Hello World" in Base64
    private final String TIMESTAMP_TOKEN = "timestamp-token-base64";

    @BeforeEach
    public void setup() {
        signingController = new SigningController(keyStorageService, sadValidator, signingChallengeService, tsaClient);
    }

    @Test
    public void testConfirmSigning_Success_WithTimestamp() throws Exception {
        // Mock Challenge Verification
        SigningChallenge challenge = new SigningChallenge(
                CHALLENGE_ID, TEST_USER, KEY_ALIAS, DOC_HASH, ALGO, Instant.now(), Instant.now().plusSeconds(300));

        VerificationResult verificationResult = VerificationResult.success(challenge);
        when(signingChallengeService.verifyChallenge(CHALLENGE_ID, OTP)).thenReturn(verificationResult);

        // Mock Key Signing
        when(keyStorageService.signHash(KEY_ALIAS, DOC_HASH, ALGO)).thenReturn(SIGNATURE);

        // Mock TSA Timestamping
        TimestampResult tsResult = mock(TimestampResult.class);
        when(tsResult.getBase64Token()).thenReturn(TIMESTAMP_TOKEN);
        when(tsaClient.timestamp(any(byte[].class))).thenReturn(tsResult);

        // Execute
        SignConfirmRequest request = new SignConfirmRequest(CHALLENGE_ID, OTP);
        ResponseEntity<?> responseEntity = signingController.confirmSigning(request);

        // Verify
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertTrue(responseEntity.getBody() instanceof SignConfirmResponse);

        SignConfirmResponse response = (SignConfirmResponse) responseEntity.getBody();
        Assertions.assertEquals(SIGNATURE, response.signatureBase64());
        Assertions.assertEquals(TIMESTAMP_TOKEN, response.timestampBase64());
        Assertions.assertEquals(KEY_ALIAS, response.keyAlias());

        // Ensure signing happened
        verify(keyStorageService).signHash(KEY_ALIAS, DOC_HASH, ALGO);
        // Ensure timestamping happened
        verify(tsaClient).timestamp(any(byte[].class));
    }

    @Test
    public void testConfirmSigning_Success_TsaFailure_ShouldStillReturnSignature() throws Exception {
        // Mock Challenge Verification
        SigningChallenge challenge = new SigningChallenge(
                CHALLENGE_ID, TEST_USER, KEY_ALIAS, DOC_HASH, ALGO, Instant.now(), Instant.now().plusSeconds(300));

        VerificationResult verificationResult = VerificationResult.success(challenge);
        when(signingChallengeService.verifyChallenge(CHALLENGE_ID, OTP)).thenReturn(verificationResult);

        // Mock Key Signing
        when(keyStorageService.signHash(KEY_ALIAS, DOC_HASH, ALGO)).thenReturn(SIGNATURE);

        // Mock TSA Failure
        when(tsaClient.timestamp(any(byte[].class))).thenThrow(new RuntimeException("TSA Down"));

        // Execute
        SignConfirmRequest request = new SignConfirmRequest(CHALLENGE_ID, OTP);
        ResponseEntity<?> responseEntity = signingController.confirmSigning(request);

        // Verify
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        SignConfirmResponse response = (SignConfirmResponse) responseEntity.getBody();

        // Critical: Signature is returned even if TSA fails (graceful degradation)
        Assertions.assertEquals(SIGNATURE, response.signatureBase64());
        // Timestamp is null because it failed
        Assertions.assertNull(response.timestampBase64());
    }

    @Test
    public void testConfirmSigning_InvalidOtp() {
        // Mock Verification Failure
        VerificationResult verificationResult = VerificationResult.failure("Invalid OTP");
        when(signingChallengeService.verifyChallenge(CHALLENGE_ID, OTP)).thenReturn(verificationResult);

        // Execute
        SignConfirmRequest request = new SignConfirmRequest(CHALLENGE_ID, OTP);
        ResponseEntity<?> responseEntity = signingController.confirmSigning(request);

        // Verify
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        ErrorResponse error = (ErrorResponse) responseEntity.getBody();
        Assertions.assertEquals("Invalid OTP", error.error());

        // Ensure NO signing happened
        try {
            verify(keyStorageService, never()).signHash(anyString(), anyString(), anyString());
        } catch (Exception e) {
        }
    }
}
