package com.gov.crypto.caauthority.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Security configuration for CA Authority service.
 * 
 * Uses Spring Session with Redis for distributed session sharing.
 * Sessions created by identity-service are accessible here.
 */
@Configuration
@EnableWebSecurity
@EnableRedisHttpSession(redisNamespace = "crypto-session")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/ca/init-csr", "/api/v1/ca/status", "/actuator/**").permitAll()
                        // Certificate endpoints require authentication
                        .requestMatchers("/api/v1/certificates/**").authenticated()
                        // All other requests permitted (internal service calls)
                        .anyRequest().permitAll());
        return http.build();
    }
}
