package com.gov.crypto.controller;

import com.gov.crypto.model.User;
import com.gov.crypto.model.BlacklistedToken;
import com.gov.crypto.identityservice.service.AuthService;
import com.gov.crypto.identityservice.service.TokenBlacklistService;
import com.gov.crypto.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final com.gov.crypto.repository.RoleRepository roleRepository;

    // JWT removed - using Redis session-based auth

    public AuthController(AuthService authService,
            AuthenticationManager authenticationManager,
            TokenBlacklistService tokenBlacklistService,
            UserRepository userRepository,
            com.gov.crypto.repository.RoleRepository roleRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    record LoginRequest(String username, String password) {
    }

    record LoginResponse(String token, Map<String, Object> user) {
    }

    record RegisterRequest(String username, String password, String email) {
    }

    record LogoutResponse(String message) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        Authentication authenticate = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        if (authenticate.isAuthenticated()) {
            // Establish session
            SecurityContextHolder.getContext().setAuthentication(authenticate);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            Map<String, Object> response = Map.of(
                    "message", "Login successful",
                    "sessionId", session.getId(),
                    "username", request.username());
            return ResponseEntity.ok(response);
        } else {
            throw new RuntimeException("invalid access");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(request.password()); // Will be encoded in service

        com.gov.crypto.model.Role role = roleRepository.findByName("CITIZEN")
                .orElseThrow(() -> new RuntimeException("Default role CITIZEN not found"));
        user.setRole(role);
        user.setIdentityStatus(User.IdentityStatus.UNVERIFIED);

        String result = authService.saveUser(user);
        return ResponseEntity.ok(Map.of("message", result, "username", request.username()));
    }

    /**
     * Logout endpoint - invalidates the session.
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
    }
}
