/**
 * TOTP Management composable
 * Provides TOTP setup and verification via CSC service
 */
export interface TotpSetupResponse {
    secret: string
    qrUri: string
    backupCodes?: string[]
}

export const useTotp = () => {
    const CSC_BASE = '/csc/v1'

    const cscFetch = <T>(endpoint: string, options: RequestInit & { body?: unknown } = {}) => {
        const { body, ...rest } = options
        return $fetch<T>(`${CSC_BASE}${endpoint}`, {
            ...rest,
            body: body as BodyInit,
            credentials: 'include'
        })
    }

    // Note: The backend uses /api/v1/credentials/totp/* endpoints
    const apiBase = '/api/v1/credentials/totp'

    const setupTotp = async () => {
        return $fetch<TotpSetupResponse>(`${apiBase}/setup`, {
            method: 'POST',
            credentials: 'include'
        })
    }

    const verifyTotp = async (code: string, username?: string) => {
        return $fetch(`${apiBase}/verify`, {
            method: 'POST',
            body: { code, username },
            credentials: 'include'
        })
    }

    const getStatus = async () => {
        try {
            return await $fetch<{ enabled: boolean }>(`${apiBase}/status`, {
                method: 'GET',
                credentials: 'include'
            })
        } catch {
            return { enabled: false }
        }
    }

    return {
        setupTotp,
        verifyTotp,
        getStatus
    }
}
