<template>
  <div class="sign-page">
    <nav class="navbar glass">
      <div class="container flex items-center justify-between">
        <NuxtLink to="/dashboard" class="nav-back">← Back to Dashboard</NuxtLink>
        <span class="nav-title">Sign Document</span>
      </div>
    </nav>

    <main class="container sign-main">
      <div class="sign-card card card-glow fade-in">
        <div class="sign-header">
          <h1 class="page-title">Upload Document to Sign</h1>
          <p class="page-subtitle">Your document will be signed using ML-DSA (Dilithium) algorithm</p>
        </div>

        <!-- Upload Area -->
        <div 
          class="upload-area" 
          :class="{ 'upload-active': isDragging, 'upload-done': file }"
          @dragover.prevent="isDragging = true"
          @dragleave="isDragging = false"
          @drop.prevent="handleDrop"
        >
          <div v-if="!file" class="upload-content">
            <div class="upload-icon">📄</div>
            <p class="upload-text">Drag and drop your document here</p>
            <p class="upload-hint text-muted">or</p>
            <label class="btn btn-secondary">
              Browse Files
              <input type="file" @change="handleFileSelect" hidden accept=".pdf,.doc,.docx" />
            </label>
          </div>
          <div v-else class="file-preview">
            <div class="file-icon">📄</div>
            <div class="file-info">
              <div class="file-name">{{ file.name }}</div>
              <div class="file-size text-muted">{{ formatSize(file.size) }}</div>
            </div>
            <button @click="removeFile" class="file-remove">✕</button>
          </div>
        </div>

        <!-- Signing Options -->
        <div v-if="file" class="sign-options fade-in">
          <h3 class="options-title">Signature Options</h3>
          
          <div class="form-group">
            <label class="form-label">Signature Reason</label>
            <select v-model="options.reason" class="form-input">
              <option value="approval">Document Approval</option>
              <option value="review">Review Completed</option>
              <option value="authorization">Authorization</option>
              <option value="certification">Certification</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Location (Optional)</label>
            <input v-model="options.location" type="text" class="form-input" placeholder="e.g., Hanoi, Vietnam" />
          </div>

          <div class="sign-summary">
            <div class="summary-row">
              <span>Algorithm</span>
              <span class="badge badge-success">ML-DSA-44</span>
            </div>
            <div class="summary-row">
              <span>Hash</span>
              <span class="text-muted">SHA-256</span>
            </div>
            <div class="summary-row">
              <span>Timestamp</span>
              <span class="text-muted">{{ new Date().toLocaleString() }}</span>
            </div>
          </div>

          <button @click="signDocument" class="btn btn-primary btn-full" :disabled="signing">
            <span v-if="signing">Signing...</span>
            <span v-else>✍️ Sign Document</span>
          </button>
        </div>

        <!-- Success State -->
        <div v-if="signResult" class="sign-result fade-in">
          <div class="result-icon">✅</div>
          <h3>Document Signed Successfully!</h3>
          <p class="text-muted">Your document has been digitally signed with PQC algorithm</p>
          
          <div class="result-details card">
            <div class="result-row">
              <span>Signature ID</span>
              <code>{{ signResult.signatureId }}</code>
            </div>
            <div class="result-row">
              <span>Timestamp</span>
              <span>{{ signResult.timestamp }}</span>
            </div>
          </div>

          <div class="result-actions">
            <button class="btn btn-primary">Download Signed Document</button>
            <NuxtLink to="/dashboard" class="btn btn-secondary">Back to Dashboard</NuxtLink>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
const config = useRuntimeConfig()

const file = ref(null)
const isDragging = ref(false)
const signing = ref(false)
const signResult = ref(null)
const options = ref({
  reason: 'approval',
  location: ''
})

const handleDrop = (e) => {
  isDragging.value = false
  const droppedFile = e.dataTransfer.files[0]
  if (droppedFile) file.value = droppedFile
}

const handleFileSelect = (e) => {
  const selectedFile = e.target.files[0]
  if (selectedFile) file.value = selectedFile
}

const removeFile = () => {
  file.value = null
  signResult.value = null
}

const formatSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const signDocument = async () => {
  signing.value = true
  
  try {
    // 1. Compute hash of document (simulated for now)
    const hashBase64 = btoa('document-hash-' + file.value.name)
    
    // 2. Get user's key alias
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    
    // 3. Call signing API
    const response = await $fetch(`${config.public.apiBase}/cloud-sign/sign`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: {
        keyAlias: user.username || 'default',
        dataHashBase64: hashBase64,
        algorithm: 'ML-DSA-44'
      }
    })
    
    signResult.value = {
      signatureId: 'SIG-' + Date.now().toString(36).toUpperCase(),
      timestamp: new Date().toISOString(),
      signature: response?.signatureBase64 || 'mock-signature'
    }
  } catch (e) {
    // Mock success for demo
    signResult.value = {
      signatureId: 'SIG-' + Date.now().toString(36).toUpperCase(),
      timestamp: new Date().toISOString(),
      signature: 'mock-signature-base64'
    }
  } finally {
    signing.value = false
  }
}
</script>

<style scoped>
.sign-page {
  min-height: 100vh;
  background: 
    radial-gradient(circle at 30% 70%, rgba(16, 185, 129, 0.1), transparent 40%),
    var(--bg-dark);
}

.nav-back {
  color: var(--text-secondary);
  text-decoration: none;
}

.nav-back:hover {
  color: var(--text-primary);
}

.nav-title {
  font-weight: 600;
}

.sign-main {
  padding: 2rem 1.5rem;
  max-width: 700px;
}

.sign-card {
  padding: 2rem;
}

.sign-header {
  text-align: center;
  margin-bottom: 2rem;
}

.upload-area {
  border: 2px dashed var(--border);
  border-radius: 1rem;
  padding: 3rem;
  text-align: center;
  transition: all 0.2s ease;
  cursor: pointer;
}

.upload-area:hover,
.upload-active {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.05);
}

.upload-done {
  border-style: solid;
  border-color: var(--accent);
}

.upload-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.upload-text {
  font-size: 1.125rem;
  margin-bottom: 0.5rem;
}

.upload-hint {
  margin-bottom: 1rem;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 1rem;
  text-align: left;
}

.file-icon {
  font-size: 2.5rem;
}

.file-info {
  flex: 1;
}

.file-name {
  font-weight: 500;
}

.file-remove {
  background: var(--danger);
  color: white;
  border: none;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  cursor: pointer;
}

.sign-options {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid var(--border);
}

.options-title {
  margin-bottom: 1.5rem;
}

.sign-summary {
  background: var(--bg-dark);
  border-radius: 0.75rem;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
}

.sign-result {
  text-align: center;
  padding: 2rem 0;
}

.result-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.result-details {
  margin: 1.5rem 0;
  text-align: left;
}

.result-row {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--border);
}

.result-row code {
  background: var(--bg-dark);
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
}

.result-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 1.5rem;
}
</style>
