package com.gov.crypto.caauthority.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as requiring a valid TOTP code in the X-TOTP-Code header.
 * 
 * Enforced by TotpVerificationAspect.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresTotp {
}
