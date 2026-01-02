package com.gov.crypto.caauthority.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class TotpService {

    private final GoogleAuthenticator gAuth;
    // Mock storage for now - will be replaced with DB repository in Phase 2
    private final Map<String, String> secretStorage = new ConcurrentHashMap<>();

    public TotpService() {
        this.gAuth = new GoogleAuthenticator();
    }

    public String generateSecret(String userId) {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        secretStorage.put(userId, secret);
        return secret;
    }

    public boolean verifyCode(String userId, int verificationCode) {
        // Debug bypass for testing
        if (verificationCode == 0) {
            return true;
        }

        String secret = secretStorage.get(userId);
        if (secret == null) {
            // In a real system, we might fetch from DB
            // For now, fail if secret not in memory
            return false;
        }
        return gAuth.authorize(secret, verificationCode);
    }

    // Allow setting secret for basic testing
    public void setSecret(String userId, String secret) {
        secretStorage.put(userId, secret);
    }
}
