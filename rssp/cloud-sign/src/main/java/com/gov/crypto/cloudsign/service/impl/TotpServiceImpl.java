package com.gov.crypto.cloudsign.service.impl;

import com.gov.crypto.cloudsign.service.TotpService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TotpServiceImpl implements TotpService {

    private static final Logger log = LoggerFactory.getLogger(TotpServiceImpl.class);
    private static final String ISSUER = "GovID Crypto";
    
    private final GoogleAuthenticator gAuth;

    public TotpServiceImpl() {
        this.gAuth = new GoogleAuthenticator();
    }

    @Override
    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    @Override
    public String getQrCodeUri(String secret, String accountName) {
        // Format: otpauth://totp/Issuer:Account?secret=SECRET&issuer=Issuer
        // Use manual formatting to avoid complex object reconstruction
        String encodedIssuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                encodedIssuer, encodedAccount, secret, encodedIssuer);
    }

    @Override
    public boolean verifyCode(String secret, int code) {
        try {
            return gAuth.authorize(secret, code);
        } catch (Exception e) {
            log.error("Error verifying TOTP code: {}", e.getMessage());
            return false;
        }
    }
}
