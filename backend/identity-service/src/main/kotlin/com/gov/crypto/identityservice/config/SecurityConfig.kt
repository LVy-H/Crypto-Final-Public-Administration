package com.gov.crypto.identityservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // For prototype; enable in prod if using Cookies
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints - registration and login
                    .requestMatchers("/auth/register", "/auth/login", "/auth/logout").permitAll()
                    // Admin endpoints - require ADMIN or OFFICER role
                    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OFFICER")
                    // Health check endpoints
                    .requestMatchers("/actuator/health", "/error").permitAll()
                    // All other actuator endpoints require ADMIN
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    // All other requests require authentication
                    .anyRequest().authenticated()
            }
            .httpBasic { it.disable() } // Using session-based auth
            // Sessions are handled by Spring Session Redis automatically
        
        return http.build()
    }
}
