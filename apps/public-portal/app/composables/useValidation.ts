/**
 * Validation Service composable
 * Provides signature and document verification
 */
export interface VerificationResult {
    isValid: boolean
    valid?: boolean // Alias for backwards compat
    algorithm?: string
    signedAt?: string
    details?: string
    signerInfo?: {
        name?: string
        certificate?: string
    }
    message?: string
}

export const useValidation = () => {
    const { post } = useApi()

    const verifySignature = async (
        signatureBase64: string,
        dataHashBase64: string,
        publicKeyPem: string
    ) => {
        return post<VerificationResult>('/validation/verify', {
            signatureBase64,
            dataHashBase64,
            publicKeyPem
        })
    }

    const verifyCountersignature = async (stampId: string) => {
        return post<VerificationResult>('/validation/verify-stamp', { stampId })
    }

    const verifyDocument = async (documentBase64: string, signatureBase64: string) => {
        return post<VerificationResult>('/validation/verify-document', {
            document: documentBase64,
            signature: signatureBase64
        })
    }

    const validateCertificate = async (certPem: string) => {
        return post<VerificationResult>('/validation/certificate', { certPem })
    }

    const generateQrCode = async (data: string) => {
        return post<{ qrCode: string; verificationUrl: string }>('/validation/qrcode/generate', { data })
    }

    const verifyQrCode = async (qrData: string) => {
        return post<VerificationResult>('/validation/qrcode/verify', { qrData })
    }

    return {
        verifySignature,
        verifyCountersignature,
        verifyDocument,
        validateCertificate,
        generateQrCode,
        verifyQrCode
    }
}
