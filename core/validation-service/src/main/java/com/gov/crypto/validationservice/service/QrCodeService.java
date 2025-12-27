package com.gov.crypto.validationservice.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class QrCodeService {

    private static final int QR_CODE_SIZE = 300;

    /**
     * Generate a QR code image containing document verification data.
     * 
     * @param documentId    The document ID
     * @param signatureHash Hash of the document signature
     * @param timestamp     When the document was signed
     * @return PNG image bytes
     */
    public byte[] generateQrCode(String documentId, String signatureHash, long timestamp)
            throws WriterException, IOException {

        // Create verification payload
        String payload = String.format(
                "{\"docId\":\"%s\",\"hash\":\"%s\",\"ts\":%d,\"ver\":\"1.0\"}",
                documentId, signatureHash, timestamp);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix bitMatrix = qrCodeWriter.encode(payload, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generate a verification code that can be used to look up document status.
     */
    public String generateVerificationCode(String documentId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((documentId + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            // Take first 8 bytes and encode as Base64 for a short code
            byte[] shortHash = new byte[8];
            System.arraycopy(hash, 0, shortHash, 0, 8);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(shortHash).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Parse QR code payload and extract verification data.
     */
    public Map<String, Object> parseQrPayload(String payload) {
        // Simple JSON parsing (in production, use Jackson or Gson)
        Map<String, Object> result = new HashMap<>();

        // Extract docId
        int docIdStart = payload.indexOf("\"docId\":\"") + 9;
        int docIdEnd = payload.indexOf("\"", docIdStart);
        result.put("docId", payload.substring(docIdStart, docIdEnd));

        // Extract hash
        int hashStart = payload.indexOf("\"hash\":\"") + 8;
        int hashEnd = payload.indexOf("\"", hashStart);
        result.put("hash", payload.substring(hashStart, hashEnd));

        // Extract timestamp
        int tsStart = payload.indexOf("\"ts\":") + 5;
        int tsEnd = payload.indexOf(",", tsStart);
        if (tsEnd == -1)
            tsEnd = payload.indexOf("}", tsStart);
        result.put("timestamp", Long.parseLong(payload.substring(tsStart, tsEnd)));

        return result;
    }
}
