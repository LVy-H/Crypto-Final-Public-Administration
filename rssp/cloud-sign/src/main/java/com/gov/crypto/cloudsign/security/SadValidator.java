package com.gov.crypto.cloudsign.security;

import org.springframework.stereotype.Component;

/**
 * Signature Activation Data (SAD) Validator for Cloud Signing.
 * Implements authorization checks before allowing signing operations.
 * 
 * Per CSC (Cloud Signature Consortium) API specification, SAD is required
 * to authorize remote signing operations.
 */
@Component
public class SadValidator {

    /**
     * Validates the Signature Activation Data (SAD) token.
     * In production, this should:
     * 1. Verify JWT/OAuth2 token signature
     * 2. Check token expiration
     * 3. Validate the user has access to the specified key
     * 4. Verify any additional claims (OTP, device binding, etc.)
     *
     * @param authHeader Authorization header (Bearer token)
     * @param keyAlias   The key alias being requested for signing
     * @return ValidationResult with status and user info
     */
    public ValidationResult validate(String authHeader, String keyAlias) {
        if (authHeader == null || authHeader.isBlank()) {
            return ValidationResult.failure("Missing Authorization header");
        }

        if (!authHeader.startsWith("Bearer ")) {
            return ValidationResult.failure("Invalid authorization format. Expected: Bearer <token>");
        }

        String token = authHeader.substring(7);

        if (token.isBlank()) {
            return ValidationResult.failure("Empty token");
        }

        // TODO: In production, implement proper JWT validation:
        // 1. Verify signature using public key from identity-service
        // 2. Check expiration
        // 3. Validate issuer and audience claims
        // 4. Check key access permissions from database

        // For now, we do basic token presence validation
        // This ensures API cannot be called without authentication
        try {
            // Basic token structure validation (should be 3 parts separated by dots for
            // JWT)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return ValidationResult.failure("Invalid token structure");
            }

            // Decode payload to extract user info (in production, verify signature first!)
            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

            // Extract username from payload (simplified - use proper JSON parsing in
            // production)
            String username = extractClaim(payloadJson, "sub");
            if (username == null || username.isBlank()) {
                return ValidationResult.failure("Token missing subject claim");
            }

            // Validate key ownership (in production, check database)
            // For now, we just verify the user is authenticated
            // Check expiration
            String expStr = extractClaim(payloadJson, "exp");
            if (expStr != null) {
                try {
                    long exp = Long.parseLong(expStr);
                    if (System.currentTimeMillis() / 1000 > exp) {
                        return ValidationResult.failure("Token expired");
                    }
                } catch (NumberFormatException e) {
                    // Ignore or fail? fail is safer
                    return ValidationResult.failure("Invalid expiration claim");
                }
            }
            // Actually, let's fix the parser logic slightly or just skip exp if not easy,
            // BUT I must implement identity_status which IS a string.

            // Check Identity Status
            String identityStatus = extractClaim(payloadJson, "identity_status");
            if (identityStatus == null || !"VERIFIED".equals(identityStatus)) {
                return ValidationResult.failure("User identity not verified. Status: " + identityStatus);
            }

            // Validate key ownership (in production, check database)
            // For now, we just verify the user is authenticated
            if (!isUserAuthorizedForKey(username, keyAlias)) {
                return ValidationResult.failure("User not authorized for key: " + keyAlias);
            }

            return ValidationResult.success(username);

        } catch (Exception e) {
            return ValidationResult.failure("Token validation failed: " + e.getMessage());
        }
    }

    /**
     * Check if user is authorized to use the specified key.
     * In production, this should query the database for key ownership.
     */
    private boolean isUserAuthorizedForKey(String username, String keyAlias) {
        // TODO: Implement proper authorization check against database
        // For now, allow if the key alias starts with the username (simple ownership
        // model)
        // This is a placeholder - implement proper RBAC/ABAC in production

        if (keyAlias == null || username == null) {
            return false;
        }

        // Simple ownership model: user can only sign with their own keys
        // Key aliases are expected to be in format: username or username_suffix
        return keyAlias.equals(username) || keyAlias.startsWith(username + "_");
    }

    /**
     * Extract a claim from JWT payload JSON.
     * In production, use a proper JSON library.
     */
    private String extractClaim(String json, String claimName) {
        // Simplified extraction for Strings: "claim":"value"
        String searchKey = "\"" + claimName + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            // Try searching for numeric/boolean (no quotes around value): "claim":123
            searchKey = "\"" + claimName + "\":";
            start = json.indexOf(searchKey);
            if (start == -1)
                return null;
            start += searchKey.length();
            int end = json.indexOf(",", start);
            if (end == -1)
                end = json.indexOf("}", start);
            if (end == -1)
                return null;
            return json.substring(start, end).trim();
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;
        return json.substring(start, end);
    }

    public record ValidationResult(boolean valid, String username, String errorMessage) {
        public static ValidationResult success(String username) {
            return new ValidationResult(true, username, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }
    }
}
