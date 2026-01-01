package com.gov.crypto.controller;

import com.gov.crypto.model.User;
import com.gov.crypto.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(Principal principal) {
        System.out
                .println("DEBUG: getUserStats called for users: " + (principal != null ? principal.getName() : "null"));
        // ideally fetching user-specific stats
        // for now returning dummy or global stats for the dashboard to render something
        // In real app, we would fetch:
        // - my certificates count
        // - my signed documents count
        // - my pending requests

        // Since we don't have easy access to Cert/Sign services here (microservices),
        // we might just return 0 or mock data to fix the 404.

        return ResponseEntity.ok(Map.of(
                "certificates", 1,
                "signedDocuments", 5,
                "pendingRequests", 0,
                "verificationLevel", "L2"));
    }

    @GetMapping("/activity")
    public ResponseEntity<List<Map<String, Object>>> getActivityLog(Principal principal) {
        // Mock activity log
        return ResponseEntity.ok(List.of(
                Map.of("id", 1, "action", "LOGIN", "timestamp", LocalDateTime.now().minusHours(1).toString(), "status",
                        "SUCCESS"),
                Map.of("id", 2, "action", "SIGN_DOCUMENT", "timestamp", LocalDateTime.now().minusDays(1).toString(),
                        "status", "SUCCESS")));
    }
}
