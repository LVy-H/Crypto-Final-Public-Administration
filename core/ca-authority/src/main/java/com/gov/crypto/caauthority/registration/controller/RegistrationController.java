package com.gov.crypto.caauthority.registration.controller;

import com.gov.crypto.caauthority.registration.dto.RegistrationRequest;
import com.gov.crypto.caauthority.registration.dto.RegistrationResponse;
import com.gov.crypto.caauthority.registration.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ra")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/request")
    public ResponseEntity<?> submitRequest(@RequestBody RegistrationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Enforce Identity Verification
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }

        // Simple claim check (In production use proper JwtDecoder)
        String token = authHeader.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                // Check if "identity_status":"VERIFIED" exists in payload
                if (!payload.contains("\"identity_status\":\"VERIFIED\"")) {
                    return ResponseEntity.status(403)
                            .body("User identity not verified. Please complete verification first.");
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token");
        }

        return ResponseEntity.ok(registrationService.registerUser(request, authHeader));
    }
}
