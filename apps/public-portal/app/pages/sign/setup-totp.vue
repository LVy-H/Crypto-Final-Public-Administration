<template>
  <div class="totp-setup-page">
    <div class="page-header">
      <NuxtLink to="/sign" class="back-link">← Quay lại</NuxtLink>
      <h1>Thiết lập xác thực 2 bước (TOTP)</h1>
    </div>

    <div v-if="loading" class="loading">Đang tải...</div>

    <div v-else-if="setupComplete" class="success-card">
      <div class="success-icon">✅</div>
      <h2>TOTP đã được kích hoạt!</h2>
      <p>Từ nay, bạn cần nhập mã OTP mỗi khi ký tài liệu.</p>
      <NuxtLink to="/sign" class="btn btn-primary">Quay lại ký tài liệu</NuxtLink>
    </div>

    <div v-else class="setup-content">
      <div class="step">
        <div class="step-number">1</div>
        <div class="step-content">
          <h3>Cài đặt ứng dụng Authenticator</h3>
          <p>Tải Google Authenticator hoặc Microsoft Authenticator trên điện thoại của bạn.</p>
        </div>
      </div>

      <div class="step">
        <div class="step-number">2</div>
        <div class="step-content">
          <h3>Quét mã QR</h3>
          <div v-if="qrCodeUrl" class="qr-container">
            <img :src="qrCodeUrl" alt="TOTP QR Code" class="qr-code" />
          </div>
          <p v-else class="qr-loading">Đang tạo mã QR...</p>
          
          <div v-if="secret" class="secret-backup">
            <p class="secret-label">Hoặc nhập mã thủ công:</p>
            <code class="secret-code">{{ secret }}</code>
          </div>
        </div>
      </div>

      <div class="step">
        <div class="step-number">3</div>
        <div class="step-content">
          <h3>Xác nhận thiết lập</h3>
          <p>Nhập mã 6 số từ ứng dụng để xác nhận:</p>
          <div class="verify-form">
            <input 
              v-model="verifyCode" 
              type="text" 
              maxlength="6" 
              placeholder="000000"
              class="otp-input"
            />
            <button 
              @click="verifySetup" 
              class="btn btn-primary"
              :disabled="verifyCode.length !== 6 || verifying"
            >
              {{ verifying ? 'Đang xác nhận...' : 'Xác nhận' }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="backupCodes.length > 0" class="backup-codes">
        <h3>⚠️ Mã khôi phục</h3>
        <p>Lưu lại các mã này ở nơi an toàn. Bạn có thể sử dụng chúng nếu mất điện thoại.</p>
        <div class="codes-grid">
          <code v-for="code in backupCodes" :key="code">{{ code }}</code>
        </div>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
definePageMeta({
  middleware: 'auth'
})

const { setupTotp, verifyTotp } = useTotp()

const loading = ref(true)
const setupComplete = ref(false)
const qrCodeUrl = ref('')
const secret = ref('')
const backupCodes = ref([])
const verifyCode = ref('')
const verifying = ref(false)
const error = ref('')

const initSetup = async () => {
  try {
    loading.value = true
    const result = await setupTotp()
    qrCodeUrl.value = result.qrCodeUrl
    secret.value = result.secret
    backupCodes.value = result.backupCodes || []
  } catch (e) {
    error.value = 'Không thể thiết lập TOTP: ' + (e.data?.message || e.message)
  } finally {
    loading.value = false
  }
}

const verifySetup = async () => {
  try {
    verifying.value = true
    error.value = ''
    const result = await verifyTotp(verifyCode.value)
    if (result.valid) {
      setupComplete.value = true
    } else {
      error.value = 'Mã không hợp lệ. Vui lòng thử lại.'
    }
  } catch (e) {
    error.value = 'Xác nhận thất bại: ' + (e.data?.message || e.message)
  } finally {
    verifying.value = false
  }
}

onMounted(initSetup)
</script>

<style scoped>
.totp-setup-page {
  max-width: 500px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.back-link {
  color: #1a4d8c;
  text-decoration: none;
  font-size: 0.9rem;
}

.page-header h1 {
  font-size: 1.3rem;
  color: #1a4d8c;
  margin-top: 1rem;
  margin-bottom: 2rem;
}

.loading {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.success-card {
  text-align: center;
  padding: 3rem 2rem;
  background: #e6f4ea;
  border-radius: 8px;
}

.success-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.success-card h2 {
  color: #137333;
  margin-bottom: 1rem;
}

.setup-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.step {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.step-number {
  width: 28px;
  height: 28px;
  background: #1a4d8c;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.step-content h3 {
  font-size: 1rem;
  margin-bottom: 0.5rem;
}

.step-content p {
  font-size: 0.9rem;
  color: #666;
  margin: 0;
}

.qr-container {
  margin: 1rem 0;
  text-align: center;
}

.qr-code {
  max-width: 200px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.qr-loading {
  padding: 2rem;
  background: #f5f5f5;
  border-radius: 8px;
  text-align: center;
}

.secret-backup {
  margin-top: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.secret-label {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 0.5rem !important;
}

.secret-code {
  display: block;
  font-family: monospace;
  font-size: 0.9rem;
  background: #fff;
  padding: 0.5rem;
  border-radius: 4px;
  word-break: break-all;
}

.verify-form {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}

.otp-input {
  flex: 1;
  text-align: center;
  font-size: 1.25rem;
  letter-spacing: 0.3rem;
  padding: 0.75rem;
  border: 2px solid #ddd;
  border-radius: 6px;
}

.backup-codes {
  background: #fef7e0;
  padding: 1.25rem;
  border-radius: 8px;
  margin-top: 1rem;
}

.backup-codes h3 {
  color: #b06000;
  margin-bottom: 0.5rem;
}

.backup-codes p {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 1rem;
}

.codes-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.codes-grid code {
  background: white;
  padding: 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  text-align: center;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  text-decoration: none;
  border: none;
}

.btn-primary { background: #1a4d8c; color: white; }
.btn:disabled { background: #ccc; }

.error-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fce8e6;
  color: #c5221f;
  border-radius: 4px;
}
</style>
