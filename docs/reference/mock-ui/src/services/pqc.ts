/**
 * PQC Crypto Service - Client-Side Key Generation
 *
 * Uses @openforge-sh/liboqs (WASM) for ML-DSA key generation
 * and @peculiar/x509 for CSR creation.
 *
 * Private keys NEVER leave the browser.
 */

import { Signature } from '@openforge-sh/liboqs';
import * as x509 from '@peculiar/x509';

// Supported PQC algorithms
export type PqcAlgorithm = 'ML-DSA-44' | 'ML-DSA-65' | 'ML-DSA-87';

export interface KeyPairResult {
    publicKey: Uint8Array;
    secretKey: Uint8Array;
    algorithm: PqcAlgorithm;
}

export interface CsrResult {
    csrPem: string;
    csrBase64: string;
}

/**
 * Generate ML-DSA key pair in browser.
 * Private key stays client-side only.
 */
export async function generateKeyPair(
    algorithm: PqcAlgorithm = 'ML-DSA-65'
): Promise<KeyPairResult> {
    const sig = new Signature(algorithm);
    const keyPair = await sig.generateKeyPair();

    return {
        publicKey: keyPair.publicKey,
        secretKey: keyPair.secretKey,
        algorithm,
    };
}

/**
 * Sign data using ML-DSA private key.
 */
export async function signData(
    secretKey: Uint8Array,
    data: Uint8Array,
    algorithm: PqcAlgorithm = 'ML-DSA-65'
): Promise<Uint8Array> {
    const sig = new Signature(algorithm);
    return sig.sign(secretKey, data);
}

/**
 * Verify ML-DSA signature.
 */
export async function verifySignature(
    publicKey: Uint8Array,
    data: Uint8Array,
    signature: Uint8Array,
    algorithm: PqcAlgorithm = 'ML-DSA-65'
): Promise<boolean> {
    const sig = new Signature(algorithm);
    return sig.verify(publicKey, data, signature);
}

/**
 * Generate PKCS#10 CSR (Certificate Signing Request).
 *
 * The CSR is self-signed with the user's PQC private key
 * to prove possession (POP).
 */
export async function generateCsr(
    subjectDn: string,
    keyPair: KeyPairResult
): Promise<CsrResult> {
    // Build CSR structure
    // Note: @peculiar/x509 uses Web Crypto, need to wrap PQC signing
    const csrData = buildCsrData(subjectDn, keyPair.publicKey);
    const signature = await signData(keyPair.secretKey, csrData, keyPair.algorithm);

    // Combine into PKCS#10 format
    const csr = assembleCsr(csrData, signature, keyPair.algorithm);
    const csrPem = arrayToPem(csr, 'CERTIFICATE REQUEST');
    const csrBase64 = btoa(String.fromCharCode(...csr));

    return { csrPem, csrBase64 };
}

/**
 * Store private key securely in browser IndexedDB.
 * Encrypted with user's passphrase using AES-GCM.
 */
export async function storePrivateKey(
    secretKey: Uint8Array,
    userId: string,
    passphrase: string
): Promise<void> {
    const db = await openKeyStore();
    const encryptedKey = await encryptWithPassphrase(secretKey, passphrase);

    const tx = db.transaction('keys', 'readwrite');
    await tx.objectStore('keys').put({
        userId,
        encryptedKey,
        algorithm: 'AES-GCM',
        createdAt: new Date().toISOString(),
    });
    await tx.done;
}

/**
 * Retrieve and decrypt private key from IndexedDB.
 */
export async function retrievePrivateKey(
    userId: string,
    passphrase: string
): Promise<Uint8Array | null> {
    const db = await openKeyStore();
    const tx = db.transaction('keys', 'readonly');
    const record = await tx.objectStore('keys').get(userId);

    if (!record) return null;

    return decryptWithPassphrase(record.encryptedKey, passphrase);
}

// ============ Internal Helpers ============

function openKeyStore(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open('pqc-keystore', 1);

        request.onupgradeneeded = () => {
            const db = request.result;
            if (!db.objectStoreNames.contains('keys')) {
                db.createObjectStore('keys', { keyPath: 'userId' });
            }
        };

        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function encryptWithPassphrase(
    data: Uint8Array,
    passphrase: string
): Promise<ArrayBuffer> {
    const encoder = new TextEncoder();
    const keyMaterial = await crypto.subtle.importKey(
        'raw',
        encoder.encode(passphrase),
        'PBKDF2',
        false,
        ['deriveKey']
    );

    const salt = crypto.getRandomValues(new Uint8Array(16));
    const key = await crypto.subtle.deriveKey(
        { name: 'PBKDF2', salt, iterations: 100000, hash: 'SHA-256' },
        keyMaterial,
        { name: 'AES-GCM', length: 256 },
        false,
        ['encrypt']
    );

    const iv = crypto.getRandomValues(new Uint8Array(12));
    const encrypted = await crypto.subtle.encrypt(
        { name: 'AES-GCM', iv },
        key,
        data
    );

    // Prepend salt + iv to encrypted data
    const result = new Uint8Array(salt.length + iv.length + encrypted.byteLength);
    result.set(salt, 0);
    result.set(iv, salt.length);
    result.set(new Uint8Array(encrypted), salt.length + iv.length);

    return result.buffer;
}

async function decryptWithPassphrase(
    encryptedData: ArrayBuffer,
    passphrase: string
): Promise<Uint8Array> {
    const data = new Uint8Array(encryptedData);
    const salt = data.slice(0, 16);
    const iv = data.slice(16, 28);
    const ciphertext = data.slice(28);

    const encoder = new TextEncoder();
    const keyMaterial = await crypto.subtle.importKey(
        'raw',
        encoder.encode(passphrase),
        'PBKDF2',
        false,
        ['deriveKey']
    );

    const key = await crypto.subtle.deriveKey(
        { name: 'PBKDF2', salt, iterations: 100000, hash: 'SHA-256' },
        keyMaterial,
        { name: 'AES-GCM', length: 256 },
        false,
        ['decrypt']
    );

    const decrypted = await crypto.subtle.decrypt(
        { name: 'AES-GCM', iv },
        key,
        ciphertext
    );

    return new Uint8Array(decrypted);
}

function buildCsrData(subjectDn: string, publicKey: Uint8Array): Uint8Array {
    // Simplified CSR structure - in production use proper ASN.1 encoding
    const encoder = new TextEncoder();
    const subjectBytes = encoder.encode(subjectDn);

    const result = new Uint8Array(subjectBytes.length + publicKey.length + 8);
    // Version
    result[0] = 0x30; // SEQUENCE
    result[1] = 0x00; // Version 0

    // Subject
    result.set(subjectBytes, 2);

    // Public Key
    result.set(publicKey, 2 + subjectBytes.length);

    return result;
}

function assembleCsr(
    csrData: Uint8Array,
    signature: Uint8Array,
    _algorithm: PqcAlgorithm
): Uint8Array {
    // Combine CSR data + signature into PKCS#10 format
    const result = new Uint8Array(csrData.length + signature.length + 4);
    result.set(csrData, 0);
    result.set(signature, csrData.length);
    return result;
}

function arrayToPem(data: Uint8Array, label: string): string {
    const base64 = btoa(String.fromCharCode(...data));
    const lines = base64.match(/.{1,64}/g) || [];
    return `-----BEGIN ${label}-----\n${lines.join('\n')}\n-----END ${label}-----`;
}

export default {
    generateKeyPair,
    signData,
    verifySignature,
    generateCsr,
    storePrivateKey,
    retrievePrivateKey,
};
