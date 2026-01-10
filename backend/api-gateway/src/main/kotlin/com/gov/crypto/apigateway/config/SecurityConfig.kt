package com.gov.crypto.apigateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints - Auth (registration, login, logout)
                    .pathMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                    
                    // Admin endpoints - require ADMIN or OFFICER role
                    .pathMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "OFFICER")
                    
                    // PKI Admin endpoints - require CA_OPERATOR or ADMIN role
                    .pathMatchers("/api/v1/pki/admin/**").hasAnyRole("CA_OPERATOR", "ADMIN")
                    
                    // Public PKI endpoints - CA certificate download
                    .pathMatchers("/api/v1/pki/ca/certificate").permitAll()
                    
                    // PKI enrollment - requires authentication
                    .pathMatchers("/api/v1/pki/**").authenticated()
                    
                    // TSA endpoints - public for timestamping
                    .pathMatchers("/api/v1/tsa/**").permitAll()
                    
                    // Document verification - public (anyone can verify)
                    .pathMatchers("/api/v1/documents/verify-asic").permitAll()
                    
                    // Document operations - require authentication (ABAC enforced at service level)
                    .pathMatchers("/api/v1/documents/**").authenticated()
                    
                    // Health check endpoints
                    .pathMatchers("/actuator/health").permitAll()
                    
                    // Other actuator endpoints require ADMIN
                    .pathMatchers("/actuator/**").hasRole("ADMIN")
                    
                    // All other requests require authentication
                    .anyExchange().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
            .build()
    }
}
