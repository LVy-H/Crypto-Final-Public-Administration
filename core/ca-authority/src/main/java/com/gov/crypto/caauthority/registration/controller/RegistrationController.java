package com.gov.crypto.caauthority.registration.controller;

import com.gov.crypto.caauthority.registration.dto.RegistrationRequest;
import com.gov.crypto.caauthority.registration.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ra")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/request")
    public ResponseEntity<?> submitRequest(@RequestBody RegistrationRequest request) {
        // Get username from Spring Security context (session-based auth)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        String username = auth.getName();

        // TODO: Check identity verification status from user service
        // For now, allow authenticated users to submit requests
        // In production, verify identity_status via identity-service API call

        return ResponseEntity.ok(registrationService.registerUser(request, username));
    }
}
