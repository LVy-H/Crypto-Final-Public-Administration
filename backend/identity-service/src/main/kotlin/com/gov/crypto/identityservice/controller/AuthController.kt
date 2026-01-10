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
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<String> {
        // Allow requesting a role (e.g. OFFICER), default to USER
        val role = request.role ?: "USER"
        authService.register(request.username, request.password, role)
        return ResponseEntity.ok("User registered successfully with role $role")
    }

    // New DTO for registration
    data class RegisterRequest(val username: String, val password: String, val role: String? = null)

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
