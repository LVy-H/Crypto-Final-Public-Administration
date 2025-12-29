package com.gov.crypto.identityservice.service;

import com.gov.crypto.model.User;
import com.gov.crypto.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    // JWT removed - using Redis session-based auth

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public String saveUser(User user) {
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        repository.save(user);
        return "User added to system";
    }

    // generateToken removed - sessions handle authentication
}
