/**
 * Certificate management composable
 * Provides API integration for certificate operations
 */
export interface Certificate {
    id: string
    serialNumber: string
    subjectName: string
    issuerName: string
    algorithm: string
    validFrom: string
    validTo: string
    status: 'PENDING' | 'ACTIVE' | 'REVOKED' | 'EXPIRED'
    certificatePem?: string
}

export interface CertificateRequest {
    id: string
    status: 'PENDING' | 'APPROVED' | 'REJECTED'
    certificateType: string
    keyAlgorithm: string
    requestedAt: string
    username?: string
}

export const useCertificates = () => {
    const { get, post } = useApi()

    // User certificate operations
    const requestCertificate = async (certificateType: string = 'SIGNATURE', keyAlgorithm: string = 'ML-DSA-65') => {
        return post<{ id: string; status: string; message: string }>('/certificates/request', {
            certificateType,
            keyAlgorithm
        })
    }

    const getMyCertificates = async () => {
        return get<Certificate[]>('/certificates/my')
    }

    const downloadCertificate = async (id: string) => {
        return get<{ certificatePem: string }>(`/certificates/${id}/download`)
    }

    // Admin certificate operations
    const getPendingRequests = async () => {
        return get<CertificateRequest[]>('/admin/certificates/requests/pending')
    }

    const approveCertificate = async (id: string) => {
        return post<{ message: string; certificatePem?: string }>(`/admin/certificates/requests/${id}/approve`)
    }

    const rejectCertificate = async (id: string, reason?: string) => {
        return post<{ message: string }>(`/admin/certificates/requests/${id}/reject`, { reason })
    }

    const getCertificateStats = async () => {
        return get<{ total: number; pending: number; active: number; revoked: number }>('/admin/certificates/stats')
    }

    return {
        // User operations
        requestCertificate,
        getMyCertificates,
        downloadCertificate,
        // Admin operations
        getPendingRequests,
        approveCertificate,
        rejectCertificate,
        getCertificateStats
    }
}
