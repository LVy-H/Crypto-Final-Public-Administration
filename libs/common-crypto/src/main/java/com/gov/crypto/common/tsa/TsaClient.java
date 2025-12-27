package com.gov.crypto.common.tsa;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.tsp.TimeStampResp;
import org.bouncycastle.tsp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * TSA (Timestamp Authority) Client for RFC 3161 timestamp requests.
 * 
 * Provides Long-Term Validation (LTV) by obtaining trusted timestamps
 * that prove when a signature was created, allowing validation even
 * after certificate expiration.
 * 
 * Usage:
 * 1. Sign document → get signature
 * 2. Call timestamp(signatureBytes) → get TimeStampToken
 * 3. Embed token in PDF's DSS dictionary
 */
public class TsaClient {

    private static final Logger log = LoggerFactory.getLogger(TsaClient.class);
    private static final String DEFAULT_HASH_ALGORITHM = "SHA-384";
    private static final ASN1ObjectIdentifier SHA384_OID = TSPAlgorithms.SHA384;

    private final String tsaUrl;
    private final String username; // Optional: for authenticated TSA
    private final String password;
    private final int timeoutMs;

    /**
     * Create TSA client with URL only (no authentication).
     */
    public TsaClient(String tsaUrl) {
        this(tsaUrl, null, null, 30000);
    }

    /**
     * Create TSA client with optional authentication.
     */
    public TsaClient(String tsaUrl, String username, String password, int timeoutMs) {
        this.tsaUrl = tsaUrl;
        this.username = username;
        this.password = password;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Result of timestamp operation.
     */
    public record TimestampResult(
            TimeStampToken token,
            byte[] encodedToken,
            java.util.Date timestampTime,
            String serialNumber) {

        /**
         * Get Base64-encoded token for storage/embedding.
         */
        public String getBase64Token() {
            return Base64.getEncoder().encodeToString(encodedToken);
        }
    }

    /**
     * Get timestamp for data (typically a signature hash).
     * 
     * @param data The data to timestamp (usually signature bytes)
     * @return TimestampResult containing the TimeStampToken
     */
    public TimestampResult timestamp(byte[] data) throws Exception {
        log.info("Requesting timestamp from TSA: {}", tsaUrl);

        // Hash the data
        MessageDigest digest = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM);
        byte[] dataHash = digest.digest(data);

        // Generate nonce for replay protection
        BigInteger nonce = new BigInteger(64, new SecureRandom());

        // Build timestamp request
        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        reqGen.setCertReq(true); // Request TSA certificate in response

        TimeStampRequest tsReq = reqGen.generate(SHA384_OID, dataHash, nonce);
        byte[] requestBytes = tsReq.getEncoded();

        log.debug("Timestamp request size: {} bytes", requestBytes.length);

        // Send request to TSA
        byte[] responseBytes = sendRequest(requestBytes);

        // Parse response
        TimeStampResp tsResp = TimeStampResp.getInstance(responseBytes);
        TimeStampResponse response = new TimeStampResponse(tsResp);

        // Validate response
        response.validate(tsReq);

        // Check status
        if (response.getStatus() != 0) {
            throw new TSPException("TSA returned error status: " + response.getStatusString());
        }

        TimeStampToken tsToken = response.getTimeStampToken();
        if (tsToken == null) {
            throw new TSPException("TSA response did not contain a timestamp token");
        }

        log.info("Timestamp obtained successfully. Time: {}, Serial: {}",
                tsToken.getTimeStampInfo().getGenTime(),
                tsToken.getTimeStampInfo().getSerialNumber());

        return new TimestampResult(
                tsToken,
                tsToken.getEncoded(),
                tsToken.getTimeStampInfo().getGenTime(),
                tsToken.getTimeStampInfo().getSerialNumber().toString());
    }

    /**
     * Get timestamp for signature and return just the encoded token.
     * Convenience method for simple use cases.
     */
    public byte[] getTimestampToken(byte[] signature) throws Exception {
        return timestamp(signature).encodedToken();
    }

    /**
     * Send HTTP request to TSA server.
     */
    private byte[] sendRequest(byte[] requestBytes) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(tsaUrl).toURL().openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/timestamp-query");
        conn.setRequestProperty("Content-Length", String.valueOf(requestBytes.length));
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);

        // Add basic auth if credentials provided
        if (username != null && password != null) {
            String auth = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes());
            conn.setRequestProperty("Authorization", "Basic " + auth);
        }

        // Send request
        try (OutputStream out = conn.getOutputStream()) {
            out.write(requestBytes);
            out.flush();
        }

        // Check response code
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("TSA returned HTTP " + responseCode);
        }

        // Read response
        try (InputStream in = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Verify that a timestamp token is valid for given data.
     */
    public boolean verifyTimestamp(byte[] data, byte[] encodedToken) throws Exception {
        // Hash the data
        MessageDigest digest = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM);
        byte[] dataHash = digest.digest(data);

        // Parse token
        TimeStampToken token = new TimeStampToken(
                new org.bouncycastle.cms.CMSSignedData(encodedToken));

        // Verify hash matches
        byte[] tokenHash = token.getTimeStampInfo().getMessageImprintDigest();

        return MessageDigest.isEqual(dataHash, tokenHash);
    }

    public String getTsaUrl() {
        return tsaUrl;
    }
}
