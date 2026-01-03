/**
 * Cloud Signing Service (CSC) composable
 * Integrates with VueUse for file handling and clipboard
 */
import { useClipboard, useFileDialog } from '@vueuse/core'
import type { SigningChallenge, SigningResult, SigningKey } from '~/schemas/signing'

export const useSigningService = () => {
    const CSC_BASE = '/csc/v1'

    // VueUse: Easy file dialog for document selection
    const {
        files,
        open: openFileDialog,
        reset: resetFiles,
        onChange: onFilesChange
    } = useFileDialog({
        accept: '.pdf,.doc,.docx,.txt',
        multiple: false
    })

    // VueUse: Copy to clipboard
    const { copy: copyToClipboard, copied } = useClipboard()

    // Base fetch with credentials
    const cscFetch = <T>(endpoint: string, options: RequestInit & { body?: unknown } = {}) => {
        const { body, ...rest } = options
        return $fetch<T>(`${CSC_BASE}${endpoint}`, {
            ...rest,
            body: body as BodyInit,
            credentials: 'include'
        })
    }

    // Generate key alias (backend requires signing_key_* pattern)
    const createKeyAlias = (suffix?: string) =>
        `signing_key_${suffix || Date.now()}`

    // Key Management
    const generateKey = async (algorithm = 'mldsa65', aliasSuffix?: string) => {
        const alias = createKeyAlias(aliasSuffix)
        return cscFetch<{ publicKeyPem: string; alias?: string }>('/keys/generate', {
            method: 'POST',
            body: { algorithm, alias }
        })
    }

    const listKeys = () =>
        cscFetch<SigningKey[]>('/keys/list', { method: 'GET' })

    const generateCsr = (alias: string, subject: string) =>
        cscFetch<{ csrPem: string }>('/keys/csr', {
            method: 'POST',
            body: { alias, subject }
        })

    // Signing Operations (2-step with TOTP)
    const initSigning = async (keyAlias: string, dataHashBase64: string, algorithm = 'SHA3-256') => {
        return cscFetch<SigningChallenge>('/sign/init', {
            method: 'POST',
            body: { keyAlias, dataHashBase64, algorithm }
        })
    }

    const confirmSigning = async (challengeId: string, otp: string) => {
        return cscFetch<SigningResult>('/sign/confirm', {
            method: 'POST',
            body: { challengeId, otp }
        })
    }

    // Direct signing (for testing/dev)
    const directSign = async (keyAlias: string, dataHashBase64: string, algorithm = 'SHA3-256') => {
        return cscFetch<SigningResult>('/sign', {
            method: 'POST',
            body: { keyAlias, dataHashBase64, algorithm }
        })
    }

    // Hash document using Web Crypto API
    const hashDocument = async (content: ArrayBuffer): Promise<string> => {
        const hashBuffer = await crypto.subtle.digest('SHA-256', content)
        const hashArray = new Uint8Array(hashBuffer)
        return btoa(String.fromCharCode.apply(null, Array.from(hashArray)))
    }

    // Read file as ArrayBuffer
    const readFileAsBuffer = (file: File): Promise<ArrayBuffer> => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader()
            reader.onload = () => resolve(reader.result as ArrayBuffer)
            reader.onerror = reject
            reader.readAsArrayBuffer(file)
        })
    }

    return {
        // VueUse utilities
        files,
        openFileDialog,
        resetFiles,
        onFilesChange,
        copyToClipboard,
        copied,
        // Key operations
        generateKey,
        listKeys,
        generateCsr,
        createKeyAlias,
        // Signing operations
        initSigning,
        confirmSigning,
        directSign,
        // Helpers
        hashDocument,
        readFileAsBuffer
    }
}
