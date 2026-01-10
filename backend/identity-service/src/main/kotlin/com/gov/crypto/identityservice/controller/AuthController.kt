package com.gov.crypto.identityservice.controller

import com.gov.crypto.identityservice.service.AuthService
import jakarta.servlet.http.HttpSession
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request: LoginRequest): ResponseEntity<String> {
        // Using LoginRequest for simplicity as it has username/password (implied) in prototype.
        // In real app, separate DTO.
        // Assuming request.username is actually "username" and we need a password field.
        // Let's assume LoginRequest is actually: data class LoginRequest(val username: String, val password: String)
        // I will update the DTO below.
        authService.register(request.username, request.password)
        return ResponseEntity.ok("User registered successfully")
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, session: HttpSession): ResponseEntity<LoginResponse> {
        if (!authService.authenticate(request.username, request.password)) {
            return ResponseEntity.status(401).body(LoginResponse("", "Invalid credentials"))
        }
        
        session.setAttribute("user", request.username)
        return ResponseEntity.ok(
            LoginResponse(
                sessionId = session.id,
                message = "Login Successful"
            )
        )
    }
}

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val sessionId: String, val message: String)
