package com.gov.crypto.caauthority.service;

import com.gov.crypto.caauthority.model.AuditLog;
import com.gov.crypto.caauthority.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log a security event.
     * Uses REQUIRES_NEW propagation to ensure the log is saved even if the main
     * transaction fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String action, String target, String details, String status) {
        String actor = "SYSTEM";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                actor = auth.getName();
            }
        } catch (Exception ignored) {
            // Fallback to SYSTEM if context unavailable
        }

        AuditLog log = new AuditLog(actor, action, target, details, status, null);
        auditLogRepository.save(log);
    }
}
