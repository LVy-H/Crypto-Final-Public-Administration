package com.gov.crypto.cloudsign.service;

import com.gov.crypto.cloudsign.model.UserTotp;
import com.gov.crypto.cloudsign.repository.UserTotpRepository;
import com.gov.crypto.cloudsign.service.SigningChallengeService.ChallengeCreatedResult;
import com.gov.crypto.cloudsign.service.SigningChallengeService.VerificationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SigningChallengeServiceTest {

    @Mock
    private UserTotpRepository userTotpRepository;

    @Mock
    private TotpService totpService;

    private SigningChallengeService signingChallengeService;

    @BeforeEach
    public void setup() {
        signingChallengeService = new SigningChallengeService(userTotpRepository, totpService);
    }

    @Test
    public void testCreateChallenge() {
        ChallengeCreatedResult result = signingChallengeService.createChallenge("user1", "alias1", "hash", "SHA256");

        Assertions.assertNotNull(result.challengeId());
        Assertions.assertNotNull(result.expiresAt());
        Assertions.assertTrue(result.expiresAt().isAfter(Instant.now()));
    }

    @Test
    public void testVerifyChallenge_Success() {
        // 1. Create Challenge
        ChallengeCreatedResult created = signingChallengeService.createChallenge("user1", "alias1", "hash", "SHA256");
        String challengeId = created.challengeId();

        // 2. Mock TOTP Setup
        UserTotp userTotp = new UserTotp("user1", "SECRET");
        when(userTotpRepository.findByUsername("user1")).thenReturn(Optional.of(userTotp));

        // 3. Mock TOTP Verification to Success
        when(totpService.verifyCode(anyString(), anyInt())).thenReturn(true);

        // 4. Verify
        VerificationResult result = signingChallengeService.verifyChallenge(challengeId, "123456");

        Assertions.assertTrue(result.valid());
        Assertions.assertNotNull(result.challenge());
        Assertions.assertEquals("user1", result.challenge().username());
    }

    @Test
    public void testVerifyChallenge_InvalidOtp() {
        // 1. Create Challenge
        ChallengeCreatedResult created = signingChallengeService.createChallenge("user1", "alias1", "hash", "SHA256");
        String challengeId = created.challengeId();

        // 2. Mock TOTP Setup
        UserTotp userTotp = new UserTotp("user1", "SECRET");
        when(userTotpRepository.findByUsername("user1")).thenReturn(Optional.of(userTotp));

        // 3. Mock TOTP Verification to Failure
        when(totpService.verifyCode(anyString(), anyInt())).thenReturn(false);

        // 4. Verify
        VerificationResult result = signingChallengeService.verifyChallenge(challengeId, "123456");

        Assertions.assertFalse(result.valid());
        Assertions.assertEquals("Invalid TOTP code", result.errorMessage());
    }

    @Test
    public void testVerifyChallenge_NoTotpSetup() {
        // 1. Create Challenge
        ChallengeCreatedResult created = signingChallengeService.createChallenge("user1", "alias1", "hash", "SHA256");
        
        // 2. Mock TOTP Not Found
        when(userTotpRepository.findByUsername("user1")).thenReturn(Optional.empty());

        // 3. Verify
        VerificationResult result = signingChallengeService.verifyChallenge(created.challengeId(), "123456");

        Assertions.assertFalse(result.valid());
        Assertions.assertEquals("TOTP not set up for user", result.errorMessage());
    }
}
