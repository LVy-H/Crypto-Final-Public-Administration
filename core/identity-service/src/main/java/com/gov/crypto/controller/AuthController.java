package com.gov.crypto.controller;

import com.gov.crypto.model.User;
import com.gov.crypto.model.BlacklistedToken;
import com.gov.crypto.identityservice.service.AuthService;
import com.gov.crypto.identityservice.service.TokenBlacklistService;
import com.gov.crypto.identityservice.service.JwtService;
import com.gov.crypto.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final com.gov.crypto.repository.RoleRepository roleRepository;

    public AuthController(AuthService authService,
            AuthenticationManager authenticationManager,
            TokenBlacklistService tokenBlacklistService,
            JwtService jwtService,
            UserRepository userRepository,
            com.gov.crypto.repository.RoleRepository roleRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtService = jwtService;
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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authenticate = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        if (authenticate.isAuthenticated()) {
            String token = authService.generateToken(request.username());
            Map<String, Object> userInfo = Map.of("username", request.username());
            return ResponseEntity.ok(new LoginResponse(token, userInfo));
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
     * Logout endpoint - blacklists the current token.
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new LogoutResponse("Invalid authorization header"));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.extractUsername(token);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get token expiration
            java.util.Date expiration = jwtService.extractExpiration(token);

            // Blacklist the token
            tokenBlacklistService.blacklistToken(
                    token,
                    user.getId(),
                    expiration.toInstant(),
                    BlacklistedToken.BlacklistReason.LOGOUT);

            return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new LogoutResponse("Failed to logout: " + e.getMessage()));
        }
    }
}
