package com.gov.crypto.identityservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

import java.util.Collections;
import java.util.List;

@Configuration
public class SessionConfig {

    /**
     * Custom Session ID Resolver to support:
     * 1. Authorization: Bearer <SESSION_ID> (Used by RegistrationService/Frontend)
     * 2. X-Auth-Token: <SESSION_ID> (Standard Spring Session)
     */
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return new HttpSessionIdResolver() {
            private final HeaderHttpSessionIdResolver xAuthResolver = HeaderHttpSessionIdResolver.xAuthToken();

            @Override
            public List<String> resolveSessionIds(HttpServletRequest request) {
                // 1. Try Authorization Bearer
                String auth = request.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    String tokenId = auth.substring(7);
                    if (!tokenId.isBlank()) {
                        return Collections.singletonList(tokenId);
                    }
                }

                // 2. Fallback to X-Auth-Token
                return xAuthResolver.resolveSessionIds(request);
            }

            @Override
            public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
                // Set both headers for compatibility
                response.setHeader("Authorization", "Bearer " + sessionId);
                xAuthResolver.setSessionId(request, response, sessionId);
            }

            @Override
            public void expireSession(HttpServletRequest request, HttpServletResponse response) {
                response.setHeader("Authorization", "");
                xAuthResolver.expireSession(request, response);
            }
        };
    }
}
