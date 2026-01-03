<template>
  <div class="verify-page">
    <div class="page-header">
      <h1>Xác thực chữ ký số</h1>
      <p class="subtitle">Kiểm tra tính hợp lệ của chữ ký và tài liệu</p>
    </div>

    <div class="verify-options">
      <div class="option-card" :class="{ active: mode === 'signature' }" @click="mode = 'signature'">
        <span class="option-icon">✍️</span>
        <h3>Xác thực chữ ký</h3>
        <p>Kiểm tra chữ ký số với dữ liệu gốc</p>
      </div>
      <div class="option-card" :class="{ active: mode === 'document' }" @click="mode = 'document'">
        <span class="option-icon">📄</span>
        <h3>Xác thực tài liệu</h3>
        <p>Kiểm tra tài liệu đã ký</p>
      </div>
      <div class="option-card" :class="{ active: mode === 'qrcode' }" @click="mode = 'qrcode'">
        <span class="option-icon">📱</span>
        <h3>Quét mã QR</h3>
        <p>Xác thực qua mã QR</p>
      </div>
    </div>

    <!-- Signature Verification -->
    <div v-if="mode === 'signature'" class="verify-form">
      <h2>Xác thực chữ ký</h2>
      <div class="form-group">
        <label>Chữ ký (Base64)</label>
        <textarea v-model="signatureForm.signature" placeholder="Dán chữ ký ở đây..." rows="4"></textarea>
      </div>
      <div class="form-group">
        <label>Dữ liệu gốc</label>
        <textarea v-model="signatureForm.data" placeholder="Dữ liệu đã ký..." rows="3"></textarea>
      </div>
      <div class="form-group">
        <label>Khóa công khai (PEM)</label>
        <textarea v-model="signatureForm.publicKey" placeholder="-----BEGIN PUBLIC KEY-----" rows="3"></textarea>
      </div>
      <button @click="verifySignature" class="btn btn-primary" :disabled="verifying">
        {{ verifying ? 'Đang xác thực...' : 'Xác thực' }}
      </button>
    </div>

    <!-- Document Verification -->
    <div v-if="mode === 'document'" class="verify-form">
      <h2>Xác thực tài liệu</h2>
      <div class="upload-area" @drop.prevent="handleDrop" @dragover.prevent>
        <input type="file" ref="fileInput" @change="handleFileSelect" accept=".pdf,.signed" hidden />
        <div class="upload-content" @click="$refs.fileInput.click()">
          <span class="upload-icon">📄</span>
          <p>Kéo thả tài liệu đã ký hoặc <strong>chọn file</strong></p>
        </div>
      </div>
      <div v-if="selectedFile" class="selected-file">
        <span>{{ selectedFile.name }}</span>
        <button @click="selectedFile = null" class="remove-file">✕</button>
      </div>
      <button v-if="selectedFile" @click="verifyDocument" class="btn btn-primary" :disabled="verifying">
        {{ verifying ? 'Đang xác thực...' : 'Xác thực tài liệu' }}
      </button>
    </div>

    <!-- QR Code Verification -->
    <div v-if="mode === 'qrcode'" class="verify-form">
      <h2>Xác thực mã QR</h2>
      <div class="form-group">
        <label>Dữ liệu mã QR</label>
        <textarea v-model="qrData" placeholder="Dán dữ liệu từ mã QR..." rows="3"></textarea>
      </div>
      <button @click="verifyQr" class="btn btn-primary" :disabled="verifying">
        {{ verifying ? 'Đang xác thực...' : 'Xác thực' }}
      </button>
    </div>

    <!-- Result -->
    <div v-if="result" class="result-card" :class="{ valid: result.valid, invalid: !result.valid }">
      <div class="result-icon">{{ result.valid ? '✅' : '❌' }}</div>
      <div class="result-content">
        <h3>{{ result.valid ? 'Chữ ký hợp lệ' : 'Chữ ký không hợp lệ' }}</h3>
        <div v-if="result.valid && result.signerInfo" class="signer-info">
          <p v-if="result.signerInfo.name"><strong>Người ký:</strong> {{ result.signerInfo.name }}</p>
          <p v-if="result.algorithm"><strong>Thuật toán:</strong> {{ result.algorithm }}</p>
          <p v-if="result.signedAt"><strong>Thời gian ký:</strong> {{ result.signedAt }}</p>
        </div>
        <p v-if="result.message" class="result-message">{{ result.message }}</p>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
const { verifySignature: verifySignatureApi, verifyDocument: verifyDocumentApi, verifyQrCode } = useValidation()

const mode = ref('signature')
const verifying = ref(false)
const result = ref(null)
const error = ref('')
const selectedFile = ref(null)
const qrData = ref('')

const signatureForm = reactive({
  signature: '',
  data: '',
  publicKey: ''
})

const handleFileSelect = (e) => {
  selectedFile.value = e.target.files[0]
  result.value = null
}

const handleDrop = (e) => {
  selectedFile.value = e.dataTransfer.files[0]
  result.value = null
}

const verifySignature = async () => {
  try {
    verifying.value = true
    error.value = ''
    result.value = null
    result.value = await verifySignatureApi(
      signatureForm.signature,
      signatureForm.data,
      signatureForm.publicKey
    )
  } catch (e) {
    error.value = 'Xác thực thất bại: ' + (e.data?.message || e.message)
  } finally {
    verifying.value = false
  }
}

const verifyDocument = async () => {
  if (!selectedFile.value) return
  try {
    verifying.value = true
    error.value = ''
    result.value = null
    
    const buffer = await selectedFile.value.arrayBuffer()
    const base64 = btoa(String.fromCharCode(...new Uint8Array(buffer)))
    
    result.value = await verifyDocumentApi(base64, '')
  } catch (e) {
    error.value = 'Xác thực thất bại: ' + (e.data?.message || e.message)
  } finally {
    verifying.value = false
  }
}

const verifyQr = async () => {
  try {
    verifying.value = true
    error.value = ''
    result.value = null
    result.value = await verifyQrCode(qrData.value)
  } catch (e) {
    error.value = 'Xác thực thất bại: ' + (e.data?.message || e.message)
  } finally {
    verifying.value = false
  }
}
</script>

<style scoped>
.verify-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.page-header h1 {
  font-size: 1.5rem;
  color: #1a4d8c;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #666;
  margin-bottom: 2rem;
}

.verify-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.option-card {
  padding: 1.25rem;
  text-align: center;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-card:hover {
  border-color: #1a4d8c;
}

.option-card.active {
  border-color: #1a4d8c;
  background: #f0f7ff;
}

.option-icon {
  font-size: 2rem;
  display: block;
  margin-bottom: 0.5rem;
}

.option-card h3 {
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
}

.option-card p {
  font-size: 0.75rem;
  color: #666;
  margin: 0;
}

.verify-form {
  background: white;
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.verify-form h2 {
  font-size: 1.1rem;
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.85rem;
  resize: vertical;
}

.upload-area {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  margin-bottom: 1rem;
}

.upload-area:hover {
  border-color: #1a4d8c;
}

.upload-icon {
  font-size: 2rem;
}

.selected-file {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.remove-file {
  background: none;
  border: none;
  cursor: pointer;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  border: none;
}

.btn-primary { background: #1a4d8c; color: white; }
.btn:disabled { background: #ccc; }

.result-card {
  display: flex;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 8px;
  margin-top: 1.5rem;
}

.result-card.valid {
  background: #e6f4ea;
  border: 1px solid #34a853;
}

.result-card.invalid {
  background: #fce8e6;
  border: 1px solid #ea4335;
}

.result-icon {
  font-size: 2rem;
}

.result-content h3 {
  margin-bottom: 0.5rem;
}

.signer-info {
  font-size: 0.9rem;
}

.signer-info p {
  margin: 0.25rem 0;
}

.result-message {
  color: #666;
  font-size: 0.85rem;
  margin-top: 0.5rem;
}

.error-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fce8e6;
  color: #c5221f;
  border-radius: 4px;
}

@media (max-width: 600px) {
  .verify-options {
    grid-template-columns: 1fr;
  }
}
</style>
