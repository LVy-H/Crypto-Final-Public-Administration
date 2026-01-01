package com.gov.crypto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
// Implementation of Admin Stats for Dashboard
public class AdminController {

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        // Mock stats for Admin Dashboard
        return ResponseEntity.ok(Map.of(
                "totalUsers", 150,
                "activeCertificates", 45,
                "pendingRequests", 12,
                "signedToday", 28));
    }
}
