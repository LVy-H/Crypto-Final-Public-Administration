package com.gov.crypto.cloudsign.service.impl;

import com.gov.crypto.cloudsign.service.TotpService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TotpServiceImplTest {

    private final TotpService totpService = new TotpServiceImpl();
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @Test
    public void testGenerateSecret() {
        String secret = totpService.generateSecret();
        Assertions.assertNotNull(secret);
        Assertions.assertTrue(secret.length() > 0);
    }

    @Test
    public void testGetQrCodeUri() {
        String secret = "JBSWY3DPEHPK3PXP"; // Example base32
        String uri = totpService.getQrCodeUri(secret, "user@example.com");
        
        // Expected: otpauth://totp/GovID+Crypto:user%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=GovID+Crypto
        Assertions.assertNotNull(uri);
        Assertions.assertTrue(uri.startsWith("otpauth://totp/"));
        Assertions.assertTrue(uri.contains("secret=" + secret));
    }

    @Test
    public void testVerifyCode() {
        // Generate a real secret
        String secret = totpService.generateSecret();
        
        // Generate a valid code using library
        int code = gAuth.getTotpPassword(secret);
        
        // Verify
        boolean isValid = totpService.verifyCode(secret, code);
        Assertions.assertTrue(isValid, "Code should be valid");
        
        // Verify invalid code
        boolean isInvalid = totpService.verifyCode(secret, code + 123456); // Assuming collision unlikely
        // Note: Simple +1 might be valid if window is large, but +123456 is definitely different code or verify window.
        // Actually gAuth tolerates window.
        // Let's use 000000
        Assertions.assertFalse(totpService.verifyCode(secret, 0), "0 should be invalid");
    }
}
