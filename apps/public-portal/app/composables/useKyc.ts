/**
 * KYC/Identity Verification composable
 * Provides API integration for identity verification operations
 */
export type IdentityStatus = 'UNVERIFIED' | 'PENDING' | 'VERIFIED' | 'REJECTED'

export interface KycRequest {
    username: string
    email: string
    status: IdentityStatus
    requestedAt?: string
}

export const useKyc = () => {
    const { get, post } = useApi()

    // User operations
    const getMyStatus = async () => {
        return get<{ status: IdentityStatus; message?: string }>('/identity/status')
    }

    const submitVerificationRequest = async (documentData?: Record<string, unknown>) => {
        return post<{ message: string; status: IdentityStatus }>('/identity/verify-request', documentData || {})
    }

    // Admin operations
    const getPendingRequests = async () => {
        return get<KycRequest[]>('/identity/pending')
    }

    const approveVerification = async (username: string) => {
        return post<{ message: string; status: string }>(`/identity/approve/${username}`)
    }

    return {
        getMyStatus,
        submitVerificationRequest,
        getPendingRequests,
        approveVerification
    }
}
