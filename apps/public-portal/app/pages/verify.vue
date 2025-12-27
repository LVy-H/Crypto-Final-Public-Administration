<template>
  <div class="page">
    <header class="gov-header">
      <div class="header-content">
        <div class="header-left">
          <span class="logo">🏛️</span>
          <h1>Hệ thống Chữ ký số Lượng tử</h1>
        </div>
        <div class="header-right">
          <NuxtLink to="/dashboard" class="btn-link">← Bảng điều khiển</NuxtLink>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
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
          <textarea v-model="signatureData" placeholder="Dán chữ ký tách rời tại đây..." rows="4" class="textarea"></textarea>
        </div>

        <button @click="verifySignature" class="btn-primary" :disabled="loading || (!selectedFile && !signatureData)">
          {{ loading ? 'Đang xác thực...' : 'Xác thực chữ ký' }}
        </button>

        <div v-if="verificationResult" class="result-box" :class="verificationResult.valid ? 'success' : 'error'">
          <h4>{{ verificationResult.valid ? '✓ Chữ ký hợp lệ' : '✗ Chữ ký không hợp lệ' }}</h4>
          <table class="info-table" v-if="verificationResult.valid">
            <tr><th>Người ký</th><td>{{ verificationResult.signer }}</td></tr>
            <tr><th>Thời gian</th><td>{{ verificationResult.timestamp }}</td></tr>
            <tr><th>Thuật toán</th><td>{{ verificationResult.algorithm }}</td></tr>
          </table>
          <p v-if="verificationResult.message" class="error-text">{{ verificationResult.message }}</p>
        </div>
      </div>
    </main>

    <footer class="gov-footer">
      <p>© 2024 Hệ thống Chữ ký số Lượng tử - Nguyễn Trọng Nhân & Lê Việt Hoàng</p>
    </footer>
  </div>
</template>

<script setup>
const config = useRuntimeConfig()

const fileInput = ref(null)
const selectedFile = ref(null)
const signatureData = ref('')
const loading = ref(false)
const verificationResult = ref(null)

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
    const formData = new FormData()
    if (selectedFile.value) formData.append('document', selectedFile.value)
    if (signatureData.value) formData.append('signature', signatureData.value)

    const response = await fetch(`${config.public.apiBase}/validation/verify`, {
      method: 'POST',
      body: formData,
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })

    if (response.ok) {
      const data = await response.json()
      verificationResult.value = { valid: data.valid, signer: data.signerSubject, timestamp: data.signedAt, algorithm: data.algorithm }
    } else {
      verificationResult.value = { valid: false, message: 'Xác thực thất bại: ' + (await response.text()) }
    }
  } catch (error) {
    verificationResult.value = { valid: false, message: 'Không thể kết nối đến dịch vụ xác thực' }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; display: flex; flex-direction: column; background: #f5f5f5; }
.gov-header { background: #1a4d8c; color: white; padding: 12px 24px; border-bottom: 3px solid #c41e3a; }
.header-content { display: flex; justify-content: space-between; align-items: center; max-width: 1200px; margin: 0 auto; }
.header-left { display: flex; align-items: center; gap: 12px; }
.logo { font-size: 24px; }
.gov-header h1 { font-size: 18px; font-weight: 600; }
.btn-link { color: white; text-decoration: underline; background: none; border: none; cursor: pointer; }
.main-content { flex: 1; padding: 24px; }
.container { max-width: 800px; margin: 0 auto; }
.page-title { font-size: 20px; font-weight: 600; margin-bottom: 24px; color: #1a4d8c; }
.section { background: white; border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; }
.section h3 { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #333; }
.upload-zone { border: 2px dashed #ccc; padding: 32px; text-align: center; cursor: pointer; background: #fafafa; }
.upload-zone:hover { border-color: #1a4d8c; background: #f0f4f8; }
.upload-zone .hint { font-size: 12px; color: #888; margin-top: 8px; }
.selected-file { display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #f8f9fa; margin-top: 12px; border: 1px solid #ddd; }
.btn-clear { background: none; border: none; color: #c41e3a; cursor: pointer; font-size: 13px; }
.textarea { width: 100%; padding: 12px; border: 1px solid #ddd; font-family: monospace; font-size: 13px; resize: vertical; }
.btn-primary { background: #1a4d8c; color: white; border: none; padding: 12px 24px; font-size: 14px; cursor: pointer; width: 100%; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }
.result-box { padding: 20px; margin-top: 20px; border: 1px solid; }
.result-box.success { background: #d4edda; border-color: #c3e6cb; }
.result-box.error { background: #f8d7da; border-color: #f5c6cb; }
.result-box h4 { margin-bottom: 12px; }
.info-table { width: 100%; }
.info-table th, .info-table td { padding: 8px 12px; text-align: left; border-bottom: 1px solid rgba(0,0,0,0.1); }
.info-table th { width: 120px; color: #555; font-weight: 500; }
.error-text { color: #721c24; margin-top: 8px; }
.gov-footer { background: #333; color: #ccc; text-align: center; padding: 16px; font-size: 13px; }
</style>
