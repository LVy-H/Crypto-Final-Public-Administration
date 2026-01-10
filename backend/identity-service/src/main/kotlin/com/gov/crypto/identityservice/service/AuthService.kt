package com.gov.crypto.identityservice.service

import com.gov.crypto.identityservice.entity.User
import com.gov.crypto.identityservice.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun register(username: String, passwordRaw: String): User {
        if (userRepository.findByUsername(username) != null) {
            throw IllegalArgumentException("Username already exists")
        }
        
        val hash = passwordEncoder.encode(passwordRaw)
        val user = User(
            username = username, 
            passwordHash = hash!!,
            checkSum = "SHA3-256-placeholder"
        )
        return userRepository.save(user)
    }

    fun authenticate(username: String, passwordRaw: String): Boolean {
        val user = userRepository.findByUsername(username) ?: return false
        return passwordEncoder.matches(passwordRaw, user.passwordHash)
    }
}
