/**
 * PQC Crypto Service - Client-Side Key Generation
 *
 * Uses @openforge-sh/liboqs (WASM) for ML-DSA key generation.
 * Private keys NEVER leave the browser.
 */

import { createMLDSA44, createMLDSA65, createMLDSA87, createSlhDsaShake128f } from '@openforge-sh/liboqs/sig';

// Supported PQC algorithms
export type PqcAlgorithm = 'ML-DSA-44' | 'ML-DSA-65' | 'ML-DSA-87' | 'SLH-DSA-SHAKE-128F';

export interface KeyPairResult {
    publicKey: Uint8Array;
    secretKey: Uint8Array;
    algorithm: PqcAlgorithm;
}

export interface CsrResult {
    csrBase64: string;
}

/**
 * Generate ML-DSA key pair in browser.
 * Private key stays client-side only.
 */
export async function generateKeyPair(
    algorithm: PqcAlgorithm = 'ML-DSA-65'
): Promise<KeyPairResult> {
    let sig;

    switch (algorithm) {
        case 'ML-DSA-44':
            sig = await createMLDSA44();
            break;
        case 'ML-DSA-65':
            sig = await createMLDSA65();
            break;
        case 'ML-DSA-87':
            sig = await createMLDSA87();
            break;
        case 'SLH-DSA-SHAKE-128F':
            sig = await createSlhDsaShake128f();
            break;
    }

    const keyPair = sig.generateKeyPair();
    sig.destroy(); // Clean up WASM resources

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
    message: Uint8Array,
    algorithm: PqcAlgorithm = 'ML-DSA-65'
): Promise<Uint8Array> {
    let sig;

    switch (algorithm) {
        case 'ML-DSA-44':
            sig = await createMLDSA44();
            break;
        case 'ML-DSA-65':
            sig = await createMLDSA65();
            break;
        case 'ML-DSA-87':
            sig = await createMLDSA87();
            break;
        case 'SLH-DSA-SHAKE-128F':
            sig = await createSlhDsaShake128f();
            break;
    }

    const signature = sig.sign(message, secretKey);
    sig.destroy();

    return signature;
}

/**
 * Verify ML-DSA signature.
 */
export async function verifySignature(
    publicKey: Uint8Array,
    message: Uint8Array,
    signature: Uint8Array,
    algorithm: PqcAlgorithm = 'ML-DSA-65'
): Promise<boolean> {
    let sig;

    switch (algorithm) {
        case 'ML-DSA-44':
            sig = await createMLDSA44();
            break;
        case 'ML-DSA-65':
            sig = await createMLDSA65();
            break;
        case 'ML-DSA-87':
            sig = await createMLDSA87();
            break;
        case 'SLH-DSA-SHAKE-128F':
            sig = await createSlhDsaShake128f();
            break;
    }

    const isValid = sig.verify(message, signature, publicKey);
    sig.destroy();

    return isValid;
}

/**
 * Generate CSR data for backend submission.
 * Returns Base64 encoded data that backend can parse.
 */
export async function generateCsrData(
    subjectDn: string,
    keyPair: KeyPairResult
): Promise<CsrResult> {
    // Build CSR-like structure (simplified for prototype)
    const encoder = new TextEncoder();
    const subjectBytes = encoder.encode(subjectDn);

    // Create data to sign: subject DN + public key
    const dataToSign = new Uint8Array(subjectBytes.length + keyPair.publicKey.length);
    dataToSign.set(subjectBytes, 0);
    dataToSign.set(keyPair.publicKey, subjectBytes.length);

    // Sign with private key (POP - Proof of Possession)
    const signature = await signData(keyPair.secretKey, dataToSign, keyPair.algorithm);

    // Combine into package: [subjectLen][subject][pubKeyLen][pubKey][sigLen][sig]
    const pkg = new Uint8Array(4 + subjectBytes.length + 4 + keyPair.publicKey.length + 4 + signature.length);
    const view = new DataView(pkg.buffer);

    let offset = 0;
    view.setUint32(offset, subjectBytes.length, true); offset += 4;
    pkg.set(subjectBytes, offset); offset += subjectBytes.length;

    view.setUint32(offset, keyPair.publicKey.length, true); offset += 4;
    pkg.set(keyPair.publicKey, offset); offset += keyPair.publicKey.length;

    view.setUint32(offset, signature.length, true); offset += 4;
    pkg.set(signature, offset);

    return {
        csrBase64: btoa(String.fromCharCode(...pkg)),
    };
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

    return new Promise((resolve, reject) => {
        const tx = db.transaction('keys', 'readwrite');
        const store = tx.objectStore('keys');
        const request = store.put({
            userId,
            encryptedKey,
            algorithm: 'AES-GCM',
            createdAt: new Date().toISOString(),
        });

        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
    });
}

/**
 * Retrieve and decrypt private key from IndexedDB.
 */
export async function retrievePrivateKey(
    userId: string,
    passphrase: string
): Promise<Uint8Array | null> {
    const db = await openKeyStore();

    return new Promise((resolve, reject) => {
        const tx = db.transaction('keys', 'readonly');
        const store = tx.objectStore('keys');
        const request = store.get(userId);

        request.onsuccess = async () => {
            const record = request.result;
            if (!record) {
                resolve(null);
                return;
            }
            try {
                const key = await decryptWithPassphrase(record.encryptedKey, passphrase);
                resolve(key);
            } catch (e) {
                reject(e);
            }
        };
        request.onerror = () => reject(request.error);
    });
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
        data as unknown as BufferSource
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

export default {
    generateKeyPair,
    signData,
    verifySignature,
    generateCsrData,
    storePrivateKey,
    retrievePrivateKey,
};
