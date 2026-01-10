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
                    // Public endpoints - Auth (registration, login)
                    .pathMatchers("/api/v1/auth/**").permitAll()
                    // Public endpoints - PKI CA info (certificate download)
                    .pathMatchers("/api/v1/pki/ca/**").permitAll()
                    // Public endpoints - TSA (RFC 3161 timestamping)
                    .pathMatchers("/api/v1/tsa/**").permitAll()
                    // Require authentication for everything else
                    .anyExchange().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
            .build()
    }
}
