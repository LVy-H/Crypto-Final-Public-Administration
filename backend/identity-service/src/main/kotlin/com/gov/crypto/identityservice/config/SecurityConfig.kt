package com.gov.crypto.identityservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
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
                    .requestMatchers("/auth/**", "/admin/**", "/actuator/**", "/error").permitAll()
                    .anyRequest().permitAll() // Relaxed for local Docker simulation
            }
            // Sessions are handled by Spring Session Redis automatically
        
        return http.build()
    }
}
