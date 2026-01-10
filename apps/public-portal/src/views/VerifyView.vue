<script setup lang="ts">
import { ref } from 'vue'
import { api } from '@/services/api'

const file = ref<File | null>(null)
const loading = ref(false)
const verificationResult = ref<{
  isValid: boolean;
  verifiedAt?: string;
  message?: string;
  details?: {
    hash: string;
    documentName: string;
    documentSize: number;
    chainStatus: string;
    signatures: Array<{
      signer: string;
      algorithm: string;
      timestamp: string;
      valid: boolean;
      message: string;
      certificateSubject?: string;
      certificateIssuer?: string;
      validFrom?: string;
      validTo?: string;
    }>;
  };
} | null>(null)
const errorMessage = ref('')

async function handleVerify() {
  if (!file.value) return
  
  loading.value = true
  verificationResult.value = null
  errorMessage.value = ''
  
  try {
    const result = await api.verifyAsic(file.value)
    
    if (result.success && result.data) {
      const data = result.data
      
      // Transform API response to match existing UI structure
      verificationResult.value = {
        isValid: data.valid,
        verifiedAt: new Date().toLocaleString('vi-VN'),
        message: data.errorMessage,
        details: {
          hash: 'SHA-256 verified',
          documentName: data.documentName,
          documentSize: data.documentSize,
          chainStatus: data.valid ? `Verified (${data.signatureCount} signatures)` : 'Verification Failed',
          signatures: data.signatures.map((sig) => ({
            signer: sig.signerName || 'Unknown',
            algorithm: 'ML-DSA (PQC)',
            timestamp: sig.timestamp || new Date().toISOString(),
            valid: sig.valid,
            message: sig.message,
            certificateSubject: sig.certificateSubject,
            certificateIssuer: sig.certificateIssuer,
            validFrom: sig.certificateNotBefore,
            validTo: sig.certificateNotAfter
          }))
        }
      }
    } else {
      errorMessage.value = result.error || 'Verification failed'
      verificationResult.value = {
        isValid: false,
        message: result.error || 'Verification request failed'
      }
    }
  } catch (e) {
    errorMessage.value = String(e)
    verificationResult.value = {
      isValid: false,
      message: String(e)
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="panel">
      <h2>Kiểm tra Chữ ký (Verification Service)</h2>

      <div class="upload-section">
        <label>Chọn file ASiC để xác thực:</label>
        <input type="file" accept=".asic,.zip" @change="e => file = (e.target as HTMLInputElement).files?.[0] || null" />
      </div>

      <button @click="handleVerify" :disabled="loading || !file" class="btn-verify">
        {{ loading ? 'Đang kiểm tra...' : 'Xác thực ngay' }}
      </button>

      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>

      <div v-if="verificationResult" class="result-box" :class="{ valid: verificationResult.isValid, invalid: !verificationResult.isValid }">
        <div class="result-header">
           <h3 v-if="verificationResult.isValid">✓ Tài liệu Hợp lệ (Valid)</h3>
           <h3 v-else>✗ Tài liệu Không hợp lệ (Invalid)</h3>
           <p v-if="verificationResult.isValid" class="verify-time">Được xác thực lúc: {{ verificationResult.verifiedAt }}</p>
        </div>

        <div class="result-details" v-if="verificationResult.isValid && verificationResult.details">
          <p><strong>Document:</strong> {{ verificationResult.details.documentName }} ({{ verificationResult.details.documentSize }} bytes)</p>
          <p v-if="verificationResult.details.chainStatus"><strong>Status:</strong> <span class="badge success">{{ verificationResult.details.chainStatus }}</span></p>

          <div class="signatures-list">
            <h4>Danh sách Chữ ký ({{ verificationResult.details.signatures?.length || 0 }} signatures):</h4>
            <div v-for="(sig, index) in verificationResult.details.signatures" :key="index" class="sig-item">
              <div class="sig-header">
                <span class="sig-index">#{{ Number(index) + 1 }}</span>
                <span class="sig-time">{{ sig.timestamp }}</span>
                <span class="sig-algo badge" :class="{ success: sig.valid, error: !sig.valid }">{{ sig.valid ? '✓ Valid' : '✗ Invalid' }}</span>
              </div>
              <div class="sig-body">
                <p><strong>Signer:</strong> {{ sig.signer }}</p>
                <p><strong>Algorithm:</strong> {{ sig.algorithm }}</p>
                <p><strong>Message:</strong> {{ sig.message }}</p>
                <p v-if="sig.certificateSubject"><strong>Certificate:</strong> {{ sig.certificateSubject }}</p>
                <p v-if="sig.validFrom"><strong>Valid:</strong> {{ sig.validFrom }} → {{ sig.validTo }}</p>
              </div>
            </div>
          </div>
        </div>
        <p v-else-if="verificationResult.message">Chi tiết: {{ verificationResult.message }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 2rem; display: flex; justify-content: center; }
.panel { background: white; padding: 2rem; width: 100%; max-width: 800px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
h2 { color: #1a4d8c; border-bottom: 2px solid #eee; padding-bottom: 1rem; margin-bottom: 1.5rem; }
.grid { display: flex; gap: 2rem; margin-bottom: 1.5rem; }
.col { flex: 1; }
.col textarea { width: 100%; height: 100px; padding: 0.5rem; }
.btn-verify { width: 100%; padding: 1rem; background: #1a4d8c; color: white; border: 0; font-size: 1.1rem; cursor: pointer; }
.btn-verify:hover { background: #153d6e; }
.result-box { margin-top: 2rem; padding: 1.5rem; text-align: center; border: 2px solid; border-radius: 8px; }
.result-box.valid { border-color: #28a745; background: #eaffea; color: #155724; }
.result-box.invalid { border-color: #dc3545; background: #ffeaea; color: #721c24; }
.verify-time { font-size: 0.9rem; color: #555; margin-bottom: 1rem; font-style: italic; }

.section-block { margin-top: 1.5rem; border: 1px solid #c3e6cb; background: #fff; border-radius: 4px; overflow: hidden; text-align: left; }
.section-title { background: #d4edda; color: #155724; padding: 0.8rem; font-weight: bold; border-bottom: 1px solid #c3e6cb; }
.section-title h4 { margin: 0; font-size: 1rem; }

.chain-list { padding: 1rem; background: #f9fff9; }
.chain-item { display: flex; gap: 1rem; margin-bottom: 1rem; padding-bottom: 1rem; border-bottom: 1px dashed #ddd; }
.chain-item:last-child { border-bottom: 0; margin-bottom: 0; padding-bottom: 0; }
.cert-icon { font-size: 1.5rem; }
.cert-info { flex: 1; }
.cert-meta { font-size: 0.85rem; color: #666; }
.cert-pem { width: 100%; height: 60px; font-family: monospace; font-size: 0.7rem; color: #888; border: 1px solid #eee; margin-top: 0.5rem; }

.signatures-list { margin-top: 1.5rem; text-align: left; }
.signatures-list h4 { margin-bottom: 1rem; border-bottom: 1px solid #ddd; padding-bottom: 0.5rem; }
.sig-item { background: #fff; border: 1px solid #eee; padding: 1rem; margin-bottom: 1rem; border-radius: 4px; }
.sig-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; background: #f1f3f5; padding: 0.5rem; border-radius: 4px; }
.sig-time { font-size: 0.9rem; color: #666; }

.badge { background: #e3f2fd; color: #0d47a1; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.8rem; }
.badge.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.sig-body textarea { width: 100%; height: 120px; margin-top: 0.3rem; margin-bottom: 0.8rem; padding: 0.5rem; font-family: monospace; font-size: 0.75rem; border: 1px solid #ddd; border-radius: 4px; white-space: pre; }
</style>
