<template>

    <div class="page-content">
      <h2 class="page-title">Ký văn bản</h2>

      <div class="section" v-if="!signResult">
        <h3>Tải lên văn bản cần ký</h3>
        <div class="upload-zone" :class="{ 'active': isDragging, 'done': file }" @dragover.prevent="isDragging = true" @dragleave="isDragging = false" @drop.prevent="handleDrop">
          <div v-if="!file">
            <p><strong>Kéo thả tệp vào đây</strong> hoặc nhấp để chọn</p>
            <p class="hint">Hỗ trợ: PDF, DOC, DOCX</p>
            <label class="btn">
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
              </select>
            </td>
          </tr>
          <tr>
            <th>Địa điểm</th>
            <td><input v-model="options.location" type="text" class="input" placeholder="VD: Hà Nội" /></td>
          </tr>
          <tr>
            <th>Khóa ký</th>
            <td>
              <select v-model="selectedKeyAlias" class="select">
                <option v-for="key in userKeys" :key="key.alias" :value="key.alias">
                  {{ key.alias }} ({{ key.algorithm }})
                </option>
              </select>
            </td>
          </tr>
          <tr><th>Thuật toán</th><td>{{ selectedKeyAlgorithm }}</td></tr>
          <tr><th>Thời gian</th><td>{{ new Date().toLocaleString('vi-VN') }}</td></tr>
        </table>
        <button @click="signDocument" class="btn-primary" :disabled="signing">
          {{ signing ? 'Đang ký...' : '✍️ Ký văn bản' }}
        </button>
      </div>

      <div class="section result-section" v-if="signResult">
        <h3>✓ Ký văn bản thành công!</h3>
        <table class="info-table">
          <tr><th>Mã chữ ký</th><td class="mono">{{ signResult.signatureId }}</td></tr>
          <tr><th>Thuật toán</th><td>{{ signResult.algorithm }}</td></tr>
          <tr><th>Thời gian</th><td>{{ signResult.timestamp }}</td></tr>
          <tr><th>Chữ ký (đầu)</th><td class="mono" style="word-break: break-all; font-size: 0.7rem;">{{ signResult.signatureBase64?.substring(0, 80) }}...</td></tr>
        </table>
        <div class="result-actions">
          <button class="btn-primary">📥 Tải văn bản đã ký</button>
          <NuxtLink to="/dashboard" class="btn">Quay lại</NuxtLink>
        </div>
      </div>
    </div>

</template>

<script setup>
definePageMeta({ middleware: 'auth' })

const config = useRuntimeConfig()
const { token, user } = useAuth()

const file = ref(null)
const isDragging = ref(false)
const signing = ref(false)
const signResult = ref(null)
const options = ref({ reason: 'approval', location: '' })

// User signing keys - mock data for now, can be fetched from API
const userKeys = ref([
  { alias: 'default', algorithm: 'ML-DSA-44' },
  { alias: user.value?.username || 'user_key', algorithm: 'ML-DSA-44' }
])
const selectedKeyAlias = ref(userKeys.value[0]?.alias || 'default')

const selectedKeyAlgorithm = computed(() => {
  const key = userKeys.value.find(k => k.alias === selectedKeyAlias.value)
  return key?.algorithm || 'ML-DSA-44'
})

const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

const handleDrop = (e) => { isDragging.value = false; const f = e.dataTransfer.files[0]; if (f) file.value = f }
const handleFileSelect = (e) => { const f = e.target.files[0]; if (f) file.value = f }
const removeFile = () => { file.value = null; signResult.value = null }
const formatSize = (bytes) => bytes < 1024 ? bytes + ' B' : bytes < 1024*1024 ? (bytes/1024).toFixed(1) + ' KB' : (bytes/(1024*1024)).toFixed(1) + ' MB'

const signDocument = async () => {
  signing.value = true
  try {
    // Convert file content to Base64
    const arrayBuffer = await file.value.arrayBuffer()
    const bytes = new Uint8Array(arrayBuffer)
    let binary = ''
    bytes.forEach(byte => binary += String.fromCharCode(byte))
    const dataBase64 = btoa(binary)
    
    const res = await fetch(`${apiBase.value}/sign/remote`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ keyAlias: selectedKeyAlias.value, dataBase64 })
    })
    
    if (!res.ok) throw new Error('Signing failed')
    
    const data = await res.json()
    signResult.value = { 
      signatureId: 'SIG-' + Date.now().toString(36).toUpperCase(), 
      timestamp: new Date().toLocaleString('vi-VN'),
      algorithm: data.algorithm || 'ML-DSA-44',
      signatureBase64: data.signatureBase64
    }
  } catch (e) {
    console.error('Signing error:', e)
    alert('Ký văn bản thất bại: ' + e.message)
    signing.value = false
    return
  }
  signing.value = false
}
</script>

<style scoped>
.page-content { max-width: 700px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }

.section { background: white; border: 1px solid #ddd; padding: 1.25rem; margin-bottom: 1rem; }
.section h3 { font-size: 0.9rem; font-weight: 600; margin-bottom: 0.75rem; }

.upload-zone { border: 2px dashed #ccc; padding: 2rem; text-align: center; cursor: pointer; background: #fafafa; }
.upload-zone:hover, .upload-zone.active { border-color: #1a4d8c; background: #f0f4f8; }
.upload-zone.done { border-style: solid; border-color: #28a745; }
.upload-zone .hint { font-size: 0.8rem; color: #888; margin: 0.5rem 0 1rem; }

.file-preview { display: flex; justify-content: space-between; align-items: center; font-size: 0.9rem; }
.btn-clear { background: none; border: none; color: #c41e3a; cursor: pointer; font-size: 0.8rem; }

.btn { display: inline-block; background: #f8f9fa; border: 1px solid #ddd; padding: 0.5rem 1rem; cursor: pointer; font-size: 0.85rem; text-decoration: none; color: #333; }
.btn:hover { background: #e9ecef; }

.info-table { width: 100%; margin-bottom: 1rem; }
.info-table th, .info-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.info-table th { width: 100px; color: #666; font-weight: 500; }
.info-table .mono { font-family: monospace; font-size: 0.8rem; }

.select, .input { width: 100%; padding: 0.5rem; border: 1px solid #ddd; font-size: 0.85rem; }

.btn-primary { background: #1a4d8c; color: white; border: none; padding: 0.75rem 1.5rem; font-size: 0.9rem; cursor: pointer; width: 100%; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #999; cursor: not-allowed; }

.result-section { background: #d4edda; border-color: #c3e6cb; }
.result-section h3 { color: #155724; }

.result-actions { display: flex; gap: 0.75rem; margin-top: 1rem; }
.result-actions .btn-primary, .result-actions .btn { width: auto; padding: 0.6rem 1rem; }
</style>
