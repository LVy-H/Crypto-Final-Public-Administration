package com.gov.crypto.cloudsign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Security configuration for cloud-sign service.
 * Uses Spring Session (Redis) for authentication - sessions are shared
 * with identity-service via Redis for SSO.
 */
@Configuration
@EnableWebSecurity
@EnableRedisHttpSession(redisNamespace = "crypto-session")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        // All CSC endpoints need auth (Principal from session)
                        .requestMatchers("/csc/v1/**").authenticated()
                        .requestMatchers("/api/v1/credentials/**").authenticated()
                        .anyRequest().authenticated())
                // Session-based auth - Principal populated from Redis session
                .build();
    }
}
