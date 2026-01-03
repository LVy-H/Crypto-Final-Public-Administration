import { z } from 'zod'

// OTP verification schema
export const otpSchema = z.object({
    otp: z.string()
        .length(6, 'OTP phải có đúng 6 số')
        .regex(/^\d+$/, 'OTP chỉ chứa số')
})

// Key generation schema
export const keyGenerationSchema = z.object({
    algorithm: z.enum(['mldsa44', 'mldsa65', 'mldsa87']).default('mldsa65'),
    alias: z.string().optional()
})

// API Response schemas
export const signingChallengeSchema = z.object({
    challengeId: z.string(),
    documentHash: z.string().optional(),
    expiresAt: z.string().optional(),
    message: z.string().optional()
})

export const signingResultSchema = z.object({
    signatureBase64: z.string(),
    signatureAlgorithm: z.string().optional(),
    timestampBase64: z.string().optional(),
    keyAlias: z.string().optional(),
    algorithm: z.string().optional()
})

export const signingKeySchema = z.object({
    alias: z.string(),
    algorithm: z.string(),
    publicKeyPem: z.string(),
    createdAt: z.string().optional()
})

// Type exports
export type OtpForm = z.infer<typeof otpSchema>
export type KeyGenerationForm = z.infer<typeof keyGenerationSchema>
export type SigningChallenge = z.infer<typeof signingChallengeSchema>
export type SigningResult = z.infer<typeof signingResultSchema>
export type SigningKey = z.infer<typeof signingKeySchema>
