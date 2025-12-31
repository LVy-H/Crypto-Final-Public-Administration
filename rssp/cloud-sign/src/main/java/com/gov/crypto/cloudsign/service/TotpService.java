package com.gov.crypto.cloudsign.service;

public interface TotpService {
    /**
     * Generate a new random secret key for TOTP.
     * @return 16-character or 32-character base32 secret.
     */
    String generateSecret();

    /**
     * Generate a Google Authenticator compatible OTPAuth URI.
     * protocol: otpauth://totp/Issuer:Account?secret=SECRET&issuer=Issuer
     * 
     * @param secret The user's secret key.
     * @param accountName The user's account name (e.g. email).
     * @return The URI to be encoded as a QR code.
     */
    String getQrCodeUri(String secret, String accountName);

    /**
     * Verify a TOTP code.
     * 
     * @param secret The user's stored secret key.
     * @param code The 6-digit code provided by the user.
     * @return true if valid, false otherwise.
     */
    boolean verifyCode(String secret, int code);
}
