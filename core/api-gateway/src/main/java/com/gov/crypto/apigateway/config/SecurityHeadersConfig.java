package com.gov.crypto.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Enterprise Security Headers Configuration.
 * 
 * Implements defense-in-depth security controls per OWASP recommendations:
 * - Content-Type validation
 * - Clickjacking protection
 * - XSS protection
 * - Content Security Policy
 * - HTTP Strict Transport Security (HSTS)
 * - Server information hiding
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * Adds enterprise security headers to all responses.
     * These headers provide protection against common web vulnerabilities.
     */
    @Bean
    public WebFilter securityHeadersWebFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Prevent MIME-sniffing attacks
            headers.add("X-Content-Type-Options", "nosniff");

            // Prevent clickjacking attacks
            headers.add("X-Frame-Options", "DENY");

            // Enable XSS filter in browser
            headers.add("X-XSS-Protection", "1; mode=block");

            // Content Security Policy - restrict resource loading
            headers.add("Content-Security-Policy",
                    "default-src 'self'; " +
                            "script-src 'self'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; " +
                            "font-src 'self'; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none';");

            // HTTP Strict Transport Security - enforce HTTPS (1 year)
            headers.add("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");

            // Prevent browser from caching sensitive responses
            headers.add("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            // Hide server implementation details
            headers.remove("Server");
            headers.add("Server", "PQC-Gateway");

            // Referrer policy - don't leak referrer to other origins
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions policy - disable unnecessary browser features
            headers.add("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=(), payment=()");

            return chain.filter(exchange);
        };
    }
}
