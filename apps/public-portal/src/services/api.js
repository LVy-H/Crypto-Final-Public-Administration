/**
 * API Service - Backend Integration Layer
 *
 * Provides typed interfaces for all backend API endpoints.
 * Handles authentication, document operations, and verification.
 */
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';
class ApiService {
    baseUrl;
    constructor(baseUrl = API_BASE_URL) {
        this.baseUrl = baseUrl;
    }
    // =========================================================================
    // Authentication
    // =========================================================================
    async register(username, password, role = 'USER') {
        try {
            const response = await fetch(`${this.baseUrl}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password, role }),
                credentials: 'include'
            });
            return { success: response.ok };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    async login(username, password) {
        try {
            const response = await fetch(`${this.baseUrl}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
                credentials: 'include'
            });
            return { success: response.ok };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    async logout() {
        await fetch(`${this.baseUrl}/auth/logout`, {
            method: 'POST',
            credentials: 'include'
        });
    }
    // =========================================================================
    // KYC Management (Admin)
    // =========================================================================
    async approveKyc(username) {
        try {
            const response = await fetch(`${this.baseUrl}/admin/approve-kyc`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, action: 'APPROVE' }),
                credentials: 'include'
            });
            return { success: response.ok };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    // =========================================================================
    // Document Operations
    // =========================================================================
    async uploadDocument(file) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            const response = await fetch(`${this.baseUrl}/documents/upload`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });
            if (response.ok) {
                const data = await response.json();
                return { success: true, data };
            }
            return { success: false, error: await response.text() };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    async getDocumentHash(docId) {
        try {
            const response = await fetch(`${this.baseUrl}/documents/${docId}/hash`, {
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.json() };
            }
            return { success: false };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    async signDocument(docId, signatureBase64, certificateBase64) {
        try {
            const response = await fetch(`${this.baseUrl}/documents/finalize-asic`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    docId,
                    signature: signatureBase64,
                    certificate: certificateBase64
                }),
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.blob() };
            }
            return { success: false, error: await response.text() };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    async countersignDocument(asicFile, signatureBase64, certificateBase64) {
        try {
            const formData = new FormData();
            formData.append('file', asicFile, 'document.asic');
            formData.append('signature', signatureBase64);
            formData.append('certificate', certificateBase64);
            const response = await fetch(`${this.baseUrl}/documents/countersign`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.blob() };
            }
            return { success: false, error: await response.text() };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    // =========================================================================
    // Verification
    // =========================================================================
    async verifyAsic(asicFile) {
        try {
            const formData = new FormData();
            formData.append('file', asicFile, 'document.asic');
            const response = await fetch(`${this.baseUrl}/documents/verify-asic`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.json() };
            }
            return { success: false, error: await response.text() };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    // =========================================================================
    // PKI Operations
    // =========================================================================
    async getCaCertificate() {
        try {
            const response = await fetch(`${this.baseUrl}/pki/ca/certificate`, {
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.blob() };
            }
            return { success: false };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    async requestCertificate(csrBase64) {
        try {
            const response = await fetch(`${this.baseUrl}/pki/ca/sign`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/pkcs10' },
                body: atob(csrBase64),
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.blob() };
            }
            return { success: false, error: await response.text() };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    // =========================================================================
    // TSA Operations
    // =========================================================================
    async getTsaInfo() {
        try {
            const response = await fetch(`${this.baseUrl}/tsa/info`, {
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.json() };
            }
            return { success: false };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
    async requestTimestamp(hash) {
        try {
            const body = hash.buffer.slice(hash.byteOffset, hash.byteOffset + hash.byteLength);
            const response = await fetch(`${this.baseUrl}/tsa/stamp`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/timestamp-query' },
                body,
                credentials: 'include'
            });
            if (response.ok) {
                return { success: true, data: await response.blob() };
            }
            return { success: false };
        }
        catch (error) {
            return { success: false, error: String(error) };
        }
    }
}
// Singleton instance
export const api = new ApiService();
// Helper functions
export function arrayBufferToBase64(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
}
export function base64ToArrayBuffer(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
}
export default api;
//# sourceMappingURL=api.js.map