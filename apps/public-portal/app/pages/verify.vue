<template>

    <div class="page-content">
      <h2 class="page-title">Xác thực chữ ký</h2>

      <div class="section">
        <h3>Tải lên văn bản cần xác thực</h3>
        <div class="upload-zone" @dragover.prevent @drop.prevent="handleDrop" @click="triggerUpload">
          <input type="file" ref="fileInput" @change="handleFileSelect" accept=".pdf,.doc,.docx" hidden />
          <p><strong>Kéo thả tệp vào đây</strong> hoặc nhấp để chọn</p>
          <p class="hint">Hỗ trợ: PDF, DOC, DOCX</p>
        </div>

        <div v-if="selectedFile" class="selected-file">
          <span>📎 {{ selectedFile.name }}</span>
          <button @click="clearFile" class="btn-clear">Xóa</button>
        </div>
      </div>

      <div class="section">
        <h3>Hoặc dán chữ ký (Base64)</h3>
        <textarea v-model="signatureData" placeholder="Dán chữ ký tách rời tại đây..." rows="3" class="textarea"></textarea>
      </div>

      <button @click="verifySignature" class="btn-primary" :disabled="loading || (!selectedFile && !signatureData)">
        {{ loading ? 'Đang xác thực...' : 'Xác thực chữ ký' }}
      </button>

      <div v-if="verificationResult" class="result-box" :class="verificationResult.valid ? 'success' : 'error'">
        <h4>{{ verificationResult.valid ? '✓ Chữ ký hợp lệ' : '✗ Chữ ký không hợp lệ' }}</h4>
        
        <div v-if="verificationResult.valid">
          <!-- Basic Info Table -->
          <table class="info-table">
            <tr><th>Người ký</th><td>{{ verificationResult.signer }}</td></tr>
            <tr><th>Thời gian ký</th><td>{{ verificationResult.timestamp }}</td></tr>
            <tr><th>Thuật toán</th><td>{{ verificationResult.algorithm }}</td></tr>
          </table>
          
          <!-- Verification Timestamp -->
          <p class="verify-time">Được xác thực lúc: {{ new Date().toLocaleString('vi-VN') }}</p>
          
          <!-- Trust Chain Section -->
          <div class="section-block" v-if="verificationResult.certificateChain && verificationResult.certificateChain.length">
            <h4>Chuỗi Tin Cậy (Trust Chain)</h4>
            <div class="chain-list">
              <div v-for="(cert, idx) in verificationResult.certificateChain" :key="idx" class="chain-item">
                <span class="cert-icon">🔐</span>
                <div class="cert-info">
                  <strong>{{ cert.subject || cert }}</strong>
                  <small v-if="cert.issuer">Cấp bởi: {{ cert.issuer }}</small>
                </div>
              </div>
            </div>
          </div>
          
          <!-- TSA Section -->
          <div class="section-block" v-if="verificationResult.tsaCertificate">
            <h4>Chứng nhận Thời gian (TSA)</h4>
            <div class="chain-list">
              <div class="chain-item">
                <span class="cert-icon">⏰</span>
                <div class="cert-info">
                  <strong>{{ verificationResult.tsaCertificate.subject || 'TSA Server' }}</strong>
                  <small>{{ verificationResult.tsaCertificate.timestamp || verificationResult.timestamp }}</small>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <p v-if="verificationResult.message" class="error-text">{{ verificationResult.message }}</p>
      </div>
    </div>

</template>

<script setup>
definePageMeta({ middleware: 'auth' })

const config = useRuntimeConfig()
const { token } = useAuth()

const fileInput = ref(null)
const selectedFile = ref(null)
const signatureData = ref('')
const loading = ref(false)
const verificationResult = ref(null)

const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

function triggerUpload() { fileInput.value?.click() }

function handleFileSelect(event) {
  const file = event.target.files[0]
  if (file) { selectedFile.value = file; verificationResult.value = null }
}

function handleDrop(event) {
  const file = event.dataTransfer.files[0]
  if (file) { selectedFile.value = file; verificationResult.value = null }
}

function clearFile() {
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
}

async function verifySignature() {
  loading.value = true
  verificationResult.value = null

  try {
    const authToken = token.value || localStorage.getItem('token')
    const formData = new FormData()
    if (selectedFile.value) formData.append('document', selectedFile.value)
    if (signatureData.value) formData.append('signature', signatureData.value)

    // Call the new verify-document endpoint
    const response = await fetch(`${apiBase.value}/validation/verify-document`, {
      method: 'POST',
      body: formData,
      headers: { 'Authorization': `Bearer ${authToken}` }
    })

    if (response.ok) {
      const data = await response.json()
      verificationResult.value = {
        valid: data.valid,
        signer: data.signerSubject,
        timestamp: data.signedAt,
        algorithm: data.algorithm,
        message: data.details,
        certificateChain: data.certificateChain || [],
        tsaCertificate: data.tsaCertificate || null
      }
    } else {
      verificationResult.value = { valid: false, message: 'Xác thực thất bại' }
    }
  } catch (error) {
    verificationResult.value = { valid: false, message: 'Không thể kết nối đến dịch vụ' }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-content { max-width: 700px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }

.section { background: white; border: 1px solid #ddd; padding: 1.25rem; margin-bottom: 1rem; }
.section h3 { font-size: 0.9rem; font-weight: 600; margin-bottom: 0.75rem; }

.upload-zone { border: 2px dashed #ccc; padding: 2rem; text-align: center; cursor: pointer; background: #fafafa; }
.upload-zone:hover { border-color: #1a4d8c; background: #f0f4f8; }
.upload-zone .hint { font-size: 0.8rem; color: #888; margin-top: 0.5rem; }

.selected-file { display: flex; justify-content: space-between; align-items: center; padding: 0.75rem; background: #f8f9fa; margin-top: 0.75rem; border: 1px solid #ddd; font-size: 0.85rem; }
.btn-clear { background: none; border: none; color: #c41e3a; cursor: pointer; font-size: 0.8rem; }

.textarea { width: 100%; padding: 0.75rem; border: 1px solid #ddd; font-family: monospace; font-size: 0.85rem; resize: vertical; }

.btn-primary { background: #1a4d8c; color: white; border: none; padding: 0.75rem 1.5rem; font-size: 0.9rem; cursor: pointer; width: 100%; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #999; cursor: not-allowed; }

.result-box { padding: 1.25rem; margin-top: 1rem; border: 1px solid; }
.result-box.success { background: #d4edda; border-color: #c3e6cb; }
.result-box.error { background: #f8d7da; border-color: #f5c6cb; }
.result-box h4 { margin-bottom: 0.75rem; font-size: 0.95rem; }

.info-table { width: 100%; }
.info-table th, .info-table td { padding: 0.5rem; text-align: left; border-bottom: 1px solid rgba(0,0,0,0.1); font-size: 0.85rem; }
.info-table th { width: 100px; color: #555; font-weight: 500; }
.error-text { color: #721c24; margin-top: 0.5rem; font-size: 0.85rem; }

.verify-time { font-size: 0.8rem; color: #666; margin-top: 0.75rem; font-style: italic; }

.section-block { margin-top: 1rem; padding: 1rem; background: rgba(255,255,255,0.5); border: 1px solid #c3e6cb; border-radius: 4px; }
.section-block h4 { font-size: 0.9rem; color: #1a4d8c; margin-bottom: 0.75rem; }

.chain-list { display: flex; flex-direction: column; gap: 0.5rem; }
.chain-item { display: flex; align-items: flex-start; gap: 0.5rem; padding: 0.5rem; background: white; border: 1px solid #ddd; border-radius: 4px; }
.cert-icon { font-size: 1.2rem; }
.cert-info { display: flex; flex-direction: column; }
.cert-info strong { font-size: 0.85rem; color: #333; }
.cert-info small { font-size: 0.75rem; color: #666; }
</style>
