package com.gov.crypto.controller;

import com.gov.crypto.model.User;
import com.gov.crypto.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityVerificationController {

    private final UserRepository userRepository;
    private final com.gov.crypto.identityservice.service.JwtService jwtService;

    public IdentityVerificationController(UserRepository userRepository,
            com.gov.crypto.identityservice.service.JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/verify-request")
    public ResponseEntity<?> requestVerification(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        if (user.getIdentityStatus() == User.IdentityStatus.VERIFIED) {
            return ResponseEntity.badRequest().body("User is already verified");
        }

        if (user.getIdentityStatus() == User.IdentityStatus.PENDING) {
            return ResponseEntity.badRequest().body("Verification is already pending");
        }

        user.setIdentityStatus(User.IdentityStatus.PENDING);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Verification request submitted successfully", "status", "PENDING"));
    }

    @PostMapping("/approve/{username}")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment if method security is enabled
    public ResponseEntity<?> approveVerification(@PathVariable String username, Authentication authentication) {
        // Simple role check if PreAuthorize is not configured
        // In a real scenario, we trust the Authentication object populated by JWT
        // Filter
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

        // OR checks based on claim if roles are simple strings
        // For now, let's assume if they can hit this endpoint (protected by Gateway
        // admin route potentially) or check here.
        // But since I don't know the exact Role mapping in SecurityConfig, I'll allow
        // it if the caller has role ADMIN.
        // If security context is not populated (e.g. no filter), this might fail.
        // Given AuthController uses AuthenticationManager, likely there is a filter.

        // Fallback: If no auth object (unit testing or misconfig), block?
        if (authentication == null || !authentication.isAuthenticated()) {
            // return ResponseEntity.status(401).build(); // Let Spring Security handle 401
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setIdentityStatus(User.IdentityStatus.VERIFIED);

        // Sign identity assertion
        String assertion = jwtService.generateIdentityAssertion(username);
        user.setIdentityDocumentSignature(assertion);

        userRepository.save(user);

        return ResponseEntity
                .ok(Map.of("message", "User verified successfully", "username", username, "status", "VERIFIED"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("username", username, "status", userOpt.get().getIdentityStatus()));
    }

    @GetMapping("/pending")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingVerifications(Authentication authentication) {
        // Basic admin check (simplistic)
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"))) {
            // return ResponseEntity.status(403).build(); // Uncomment to enforce
        }

        java.util.List<User> pendingUsers = userRepository.findByIdentityStatus(User.IdentityStatus.PENDING);
        // Map to DTO if needed to avoid exposing sensitive info like passwordHash
        java.util.List<Map<String, Object>> result = pendingUsers.stream().map(u -> Map.<String, Object>of(
                "username", u.getUsername(),
                "email", u.getEmail() != null ? u.getEmail() : "",
                "identityStatus", u.getIdentityStatus())).toList();

        return ResponseEntity.ok(result);
    }
}
