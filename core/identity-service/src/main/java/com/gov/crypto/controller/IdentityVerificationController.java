package com.gov.crypto.controller;

import com.gov.crypto.model.User;
import com.gov.crypto.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VERIFY_IDENTITY')")
    public ResponseEntity<?> approveVerification(@PathVariable String username) {
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
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VERIFY_IDENTITY')")
    public ResponseEntity<?> getPendingVerifications() {
        // Protected by @PreAuthorize - only ADMIN or users with VERIFY_IDENTITY
        // permission

        java.util.List<User> pendingUsers = userRepository.findByIdentityStatus(User.IdentityStatus.PENDING);
        // Map to DTO if needed to avoid exposing sensitive info like passwordHash
        java.util.List<Map<String, Object>> result = pendingUsers.stream().map(u -> Map.<String, Object>of(
                "username", u.getUsername(),
                "email", u.getEmail() != null ? u.getEmail() : "",
                "identityStatus", u.getIdentityStatus())).toList();

        return ResponseEntity.ok(result);
    }
}
