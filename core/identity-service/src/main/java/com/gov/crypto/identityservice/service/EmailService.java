package com.gov.crypto.identityservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@crypto.gov.vn}")
    private String fromAddress;

    @Value("${app.base-url:https://portal.crypto.gov.vn}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a simple text email.
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    /**
     * Send HTML email for verification notifications.
     */
    @Async
    public void sendVerificationNotification(String to, String username, String status) {
        String subject = "Identity Verification Update - " + status;
        String htmlContent = buildVerificationEmailContent(username, status);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Send certificate issuance notification.
     */
    @Async
    public void sendCertificateNotification(String to, String username, String certSubject) {
        String subject = "Digital Certificate Issued";
        String htmlContent = buildCertificateEmailContent(username, certSubject);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Send document signing notification.
     */
    @Async
    public void sendDocumentSignedNotification(String to, String documentName, String signedBy) {
        String subject = "Document Signed: " + documentName;
        String htmlContent = buildDocumentSignedEmailContent(documentName, signedBy);
        sendHtmlEmail(to, subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildVerificationEmailContent(String username, String status) {
        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                            <div style="background: #1a365d; color: white; padding: 20px; text-align: center;">
                                <h1>🔐 PQC Digital Signature System</h1>
                            </div>
                            <div style="padding: 20px; border: 1px solid #ddd;">
                                <h2>Identity Verification Update</h2>
                                <p>Dear <strong>%s</strong>,</p>
                                <p>Your identity verification status has been updated to: <strong style="color: %s;">%s</strong></p>
                                <p>%s</p>
                                <a href="%s/dashboard" style="display: inline-block; background: #2563eb; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Dashboard</a>
                            </div>
                            <div style="background: #f5f5f5; padding: 10px; text-align: center; font-size: 12px;">
                                <p>© 2024 Government PQC Digital Signature Authority</p>
                            </div>
                        </body>
                        </html>
                        """,
                username,
                status.equals("VERIFIED") ? "green" : (status.equals("REJECTED") ? "red" : "orange"),
                status,
                status.equals("VERIFIED")
                        ? "You can now apply for digital certificates and sign documents."
                        : (status.equals("REJECTED")
                                ? "Please contact support for more information."
                                : "Your verification is being processed."),
                baseUrl);
    }

    private String buildCertificateEmailContent(String username, String certSubject) {
        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                            <div style="background: #1a365d; color: white; padding: 20px; text-align: center;">
                                <h1>🔐 PQC Digital Signature System</h1>
                            </div>
                            <div style="padding: 20px; border: 1px solid #ddd;">
                                <h2>Certificate Issued</h2>
                                <p>Dear <strong>%s</strong>,</p>
                                <p>A new digital certificate has been issued to you:</p>
                                <p style="background: #f5f5f5; padding: 10px; border-radius: 5px;"><code>%s</code></p>
                                <p>This certificate uses quantum-resistant cryptography (ML-DSA/Dilithium).</p>
                                <a href="%s/certificates" style="display: inline-block; background: #2563eb; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Certificates</a>
                            </div>
                        </body>
                        </html>
                        """,
                username, certSubject, baseUrl);
    }

    private String buildDocumentSignedEmailContent(String documentName, String signedBy) {
        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                            <div style="background: #1a365d; color: white; padding: 20px; text-align: center;">
                                <h1>🔐 PQC Digital Signature System</h1>
                            </div>
                            <div style="padding: 20px; border: 1px solid #ddd;">
                                <h2>Document Signed</h2>
                                <p>The following document has been digitally signed:</p>
                                <p style="background: #f5f5f5; padding: 10px; border-radius: 5px;"><strong>%s</strong></p>
                                <p>Signed by: <strong>%s</strong></p>
                                <p>This signature is cryptographically verified using quantum-resistant algorithms.</p>
                                <a href="%s/documents" style="display: inline-block; background: #2563eb; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Documents</a>
                            </div>
                        </body>
                        </html>
                        """,
                documentName, signedBy, baseUrl);
    }
}
