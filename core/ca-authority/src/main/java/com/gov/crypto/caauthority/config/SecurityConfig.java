package com.gov.crypto.caauthority.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Security configuration for CA Authority service.
 * 
 * Uses Spring Session with Redis for distributed session sharing.
 * Sessions created by identity-service are accessible here via cookie.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableRedisHttpSession(redisNamespace = "crypto-session")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public PKI endpoints - CRL and certificate chain distribution
                        .requestMatchers("/api/v1/ca/status", "/actuator/**").permitAll()
                        .requestMatchers("/api/v1/ca/crl/**").permitAll() // CRL Distribution Point
                        .requestMatchers("/api/v1/ca/{caId}/chain").permitAll() // Certificate chain
                        .requestMatchers("/api/v1/ca/{caId}/cert").permitAll() // CA certificate
                        // Protected CA management endpoints
                        .requestMatchers("/api/v1/ca/**").authenticated()
                        .requestMatchers("/api/v1/officers/**").authenticated()
                        .requestMatchers("/api/v1/certificates/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        // Default deny-all policy for safety
                        .anyRequest().authenticated());
        // Removed httpBasic() - we rely on Spring Session from Redis
        // The session cookie (JSESSIONID) is validated against Redis store
        return http.build();
    }
}
