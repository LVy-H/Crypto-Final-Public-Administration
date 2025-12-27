package com.gov.crypto.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting Configuration for Enterprise Security.
 * 
 * Provides protection against:
 * - Brute force attacks on authentication endpoints
 * - DoS attacks on CA initialization endpoints
 * - Resource exhaustion from certificate issuance
 * 
 * Note: Requires Redis for distributed rate limiting in production.
 * Configure with spring.cloud.gateway.redis-rate-limiter in application.yml
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Resolves rate limit key based on client IP address.
     * Used for general API rate limiting.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(clientIp);
        };
    }

    /**
     * Resolves rate limit key based on username from Authorization header.
     * Used for authenticated endpoint rate limiting.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                // In production, decode JWT to get username
                // For now, use the token hash as key
                return Mono.just("user:" + authorization.hashCode());
            }
            // Fall back to IP for unauthenticated requests
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + clientIp);
        };
    }
}
