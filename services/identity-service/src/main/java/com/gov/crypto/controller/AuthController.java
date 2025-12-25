package com.gov.crypto.controller;

import com.gov.crypto.model.User;
import com.gov.crypto.identityservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    record LoginRequest(String username, String password) {
    }

    record LoginResponse(String token, Map<String, Object> user) {
    }

    record RegisterRequest(String username, String password, String email) {
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
        user.setRole("CITIZEN");
        user.setKycStatus("PENDING");

        String result = authService.saveUser(user);
        return ResponseEntity.ok(Map.of("message", result, "username", request.username()));
    }
}
