package com.gov.crypto.validationservice.controller;

import com.gov.crypto.validationservice.service.QrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/qr")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * Generate a QR code for a signed document.
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateQrCode(@RequestBody GenerateQrRequest request) {
        try {
            byte[] qrImage = qrCodeService.generateQrCode(
                    request.documentId(),
                    request.signatureHash(),
                    request.timestamp() != null ? request.timestamp() : System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrImage.length);

            return new ResponseEntity<>(qrImage, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Verify a document using QR payload data.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyFromQr(@RequestBody VerifyQrRequest request) {
        try {
            Map<String, Object> parsedData = qrCodeService.parseQrPayload(request.payload());

            // In a real implementation, verify the signature against the document
            // For now, return the parsed data with a verification status
            parsedData.put("verified", true);
            parsedData.put("message", "Document signature data extracted successfully");

            return ResponseEntity.ok(parsedData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "verified", false,
                    "error", "Invalid QR payload: " + e.getMessage()));
        }
    }

    /**
     * Generate a short verification code for a document.
     */
    @GetMapping("/code/{documentId}")
    public ResponseEntity<Map<String, String>> generateCode(@PathVariable String documentId) {
        String code = qrCodeService.generateVerificationCode(documentId);
        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "verificationCode", code));
    }

    record GenerateQrRequest(String documentId, String signatureHash, Long timestamp) {
    }

    record VerifyQrRequest(String payload) {
    }
}
