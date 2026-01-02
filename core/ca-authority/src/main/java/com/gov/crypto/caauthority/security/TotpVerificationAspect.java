package com.gov.crypto.caauthority.security;

import com.gov.crypto.caauthority.service.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Aspect
@Component
public class TotpVerificationAspect {

    private final TotpService totpService;

    public TotpVerificationAspect(TotpService totpService) {
        this.totpService = totpService;
    }

    @Around("@annotation(com.gov.crypto.caauthority.security.RequiresTotp)")
    public Object verifyTotp(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String totpHeader = request.getHeader("X-TOTP-Code");

        if (totpHeader == null || totpHeader.isEmpty()) {
            throw new SecurityException("Missing X-TOTP-Code header");
        }

        int verificationCode;
        try {
            verificationCode = Integer.parseInt(totpHeader);
        } catch (NumberFormatException e) {
            throw new SecurityException("Invalid TOTP code format");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Use username directly (String) instead of forcing UUID
        String userId = auth.getName();

        if (!totpService.verifyCode(userId, verificationCode)) {
            throw new SecurityException("Invalid TOTP code");
        }

        return joinPoint.proceed();
    }
}
