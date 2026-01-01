package com.gov.crypto.cloudsign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for cloud-sign service.
 * Uses Spring Session (Redis) for authentication - tokens are session IDs
 * shared via Redis.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/credentials/**").authenticated()
                        .requestMatchers("/csc/v1/**").permitAll() // Handled by SadValidator
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().permitAll())
                .build();
    }
}
