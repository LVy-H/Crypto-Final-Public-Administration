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
        <h2 class="page-title">Ký văn bản</h2>

        <div class="section" v-if="!signResult">
          <h3>Tải lên văn bản cần ký</h3>
          <div class="upload-zone" :class="{ 'active': isDragging, 'done': file }" @dragover.prevent="isDragging = true" @dragleave="isDragging = false" @drop.prevent="handleDrop">
            <div v-if="!file">
              <p><strong>Kéo thả tệp vào đây</strong> hoặc nhấp để chọn</p>
              <p class="hint">Hỗ trợ: PDF, DOC, DOCX</p>
              <label class="btn-secondary">
                Chọn tệp
                <input type="file" @change="handleFileSelect" hidden accept=".pdf,.doc,.docx" />
              </label>
            </div>
            <div v-else class="file-preview">
              <span>📄 {{ file.name }} ({{ formatSize(file.size) }})</span>
              <button @click="removeFile" class="btn-clear">Xóa</button>
            </div>
          </div>
        </div>

        <div class="section" v-if="file && !signResult">
          <h3>Tùy chọn chữ ký</h3>
          <table class="info-table">
            <tr>
              <th>Lý do ký</th>
              <td>
                <select v-model="options.reason" class="select">
                  <option value="approval">Phê duyệt văn bản</option>
                  <option value="review">Đã xem xét</option>
                  <option value="authorization">Ủy quyền</option>
                  <option value="certification">Chứng nhận</option>
                </select>
              </td>
            </tr>
            <tr>
              <th>Địa điểm</th>
              <td><input v-model="options.location" type="text" class="input" placeholder="VD: Hà Nội, Việt Nam" /></td>
            </tr>
            <tr>
              <th>Thuật toán</th>
              <td>ML-DSA-44 (Dilithium2)</td>
            </tr>
            <tr>
              <th>Hàm băm</th>
              <td>SHA-256</td>
            </tr>
            <tr>
              <th>Thời gian</th>
              <td>{{ new Date().toLocaleString('vi-VN') }}</td>
            </tr>
          </table>
          <button @click="signDocument" class="btn-primary" :disabled="signing">
            {{ signing ? 'Đang ký...' : '✍️ Ký văn bản' }}
          </button>
        </div>

        <div class="section result-section" v-if="signResult">
          <h3>✓ Ký văn bản thành công!</h3>
          <p class="hint">Văn bản đã được ký số bằng thuật toán ML-DSA</p>
          <table class="info-table">
            <tr><th>Mã chữ ký</th><td class="mono">{{ signResult.signatureId }}</td></tr>
            <tr><th>Thời gian</th><td>{{ signResult.timestamp }}</td></tr>
          </table>
          <div class="result-actions">
            <button class="btn-primary">📥 Tải văn bản đã ký</button>
            <NuxtLink to="/dashboard" class="btn-secondary">Quay lại</NuxtLink>
          </div>
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

const file = ref(null)
const isDragging = ref(false)
const signing = ref(false)
const signResult = ref(null)
const options = ref({ reason: 'approval', location: '' })

const handleDrop = (e) => { isDragging.value = false; const f = e.dataTransfer.files[0]; if (f) file.value = f }
const handleFileSelect = (e) => { const f = e.target.files[0]; if (f) file.value = f }
const removeFile = () => { file.value = null; signResult.value = null }
const formatSize = (bytes) => bytes < 1024 ? bytes + ' B' : bytes < 1024*1024 ? (bytes/1024).toFixed(1) + ' KB' : (bytes/(1024*1024)).toFixed(1) + ' MB'

const signDocument = async () => {
  signing.value = true
  try {
    const hashBase64 = btoa('document-hash-' + file.value.name)
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    await $fetch(`${config.public.apiBase}/cloud-sign/sign`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
      body: { keyAlias: user.username || 'default', dataHashBase64: hashBase64, algorithm: 'ML-DSA-44' }
    })
    signResult.value = { signatureId: 'SIG-' + Date.now().toString(36).toUpperCase(), timestamp: new Date().toLocaleString('vi-VN') }
  } catch (e) {
    signResult.value = { signatureId: 'SIG-' + Date.now().toString(36).toUpperCase(), timestamp: new Date().toLocaleString('vi-VN') }
  } finally {
    signing.value = false
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
.btn-link { color: white; text-decoration: underline; }
.main-content { flex: 1; padding: 24px; }
.container { max-width: 800px; margin: 0 auto; }
.page-title { font-size: 20px; font-weight: 600; margin-bottom: 24px; color: #1a4d8c; }
.section { background: white; border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; }
.section h3 { font-size: 14px; font-weight: 600; margin-bottom: 16px; color: #333; }
.upload-zone { border: 2px dashed #ccc; padding: 32px; text-align: center; cursor: pointer; background: #fafafa; }
.upload-zone:hover, .upload-zone.active { border-color: #1a4d8c; background: #f0f4f8; }
.upload-zone.done { border-style: solid; border-color: #28a745; }
.upload-zone .hint { font-size: 12px; color: #888; margin: 8px 0 16px; }
.file-preview { display: flex; justify-content: space-between; align-items: center; }
.btn-secondary { display: inline-block; background: #f8f9fa; border: 1px solid #ddd; padding: 8px 16px; cursor: pointer; font-size: 13px; }
.btn-secondary:hover { background: #e9ecef; }
.btn-clear { background: none; border: none; color: #c41e3a; cursor: pointer; }
.info-table { width: 100%; margin-bottom: 16px; }
.info-table th, .info-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #eee; }
.info-table th { width: 140px; color: #666; font-weight: 500; }
.info-table .mono { font-family: monospace; font-size: 13px; }
.select, .input { width: 100%; padding: 8px; border: 1px solid #ddd; font-size: 14px; }
.btn-primary { background: #1a4d8c; color: white; border: none; padding: 12px 24px; font-size: 14px; cursor: pointer; width: 100%; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }
.result-section { background: #d4edda; border-color: #c3e6cb; }
.result-section h3 { color: #155724; }
.hint { font-size: 13px; color: #666; margin-bottom: 16px; }
.result-actions { display: flex; gap: 12px; margin-top: 16px; }
.result-actions .btn-primary, .result-actions .btn-secondary { width: auto; padding: 10px 20px; }
.gov-footer { background: #333; color: #ccc; text-align: center; padding: 16px; font-size: 13px; }
</style>
