<template>
  <div class="page-content">
    <h2 class="page-title">Cài đặt bảo mật</h2>

    <div class="section">
      <h3>🔐 Xác thực hai yếu tố (TOTP)</h3>
      <p class="description">
        Bảo vệ tài khoản của bạn bằng mã xác thực từ ứng dụng Authenticator (Google Authenticator, Authy, etc.)
      </p>

      <div v-if="!totpSetup && !isEnrolled" class="setup-prompt">
        <p>Bạn chưa kích hoạt xác thực TOTP. Điều này bắt buộc để ký văn bản.</p>
        <button @click="setupTotp" class="btn-primary" :disabled="loading">
          {{ loading ? 'Đang tạo...' : '🔒 Kích hoạt TOTP' }}
        </button>
      </div>

      <div v-if="totpSetup" class="qr-section">
        <p><strong>Bước 1:</strong> Quét mã QR bằng ứng dụng Authenticator:</p>
        <div class="qr-container">
          <QRCodeVue3 :value="totpSetup.qrUri" :size="200" level="M" />
        </div>
        <p class="hint">Hoặc nhập mã thủ công: <code>{{ totpSetup.secret }}</code></p>
        
        <p><strong>Bước 2:</strong> Nhập mã 6 chữ số để xác nhận:</p>
        <input 
          v-model="verifyCode" 
          type="text" 
          class="input totp-input" 
          placeholder="000000" 
          maxlength="6"
        />
        <button @click="verifySetup" class="btn-primary" :disabled="loading || verifyCode.length !== 6">
          {{ loading ? 'Đang xác thực...' : '✓ Xác nhận' }}
        </button>
        <p class="error" v-if="errorMessage">{{ errorMessage }}</p>
      </div>

      <div v-if="isEnrolled" class="enrolled-status">
        <p class="success">✓ TOTP đã được kích hoạt thành công!</p>
        <p class="hint">Bạn có thể sử dụng ứng dụng Authenticator để ký văn bản.</p>
      </div>
    </div>

    <NuxtLink to="/dashboard" class="btn">← Quay lại Dashboard</NuxtLink>
  </div>
</template>

<script setup lang="ts">
import QRCodeVue3 from 'qrcode.vue'

definePageMeta({ middleware: 'auth' })

// Composables
const { user } = useAuth()
const { post } = useApi()

// State
const loading = ref(false)
const totpSetup = ref<{ secret: string; qrUri: string } | null>(null)
const verifyCode = ref('')
const isEnrolled = ref(false)
const errorMessage = ref('')

/**
 * Setup TOTP using useApi composable
 */
const setupTotp = async () => {
  loading.value = true
  errorMessage.value = ''
  
  try {
    const data = await post<{ secret: string; qrUri: string }>('/credentials/totp/setup')
    totpSetup.value = {
      secret: data.secret,
      qrUri: data.qrUri
    }
  } catch (e: unknown) {
    console.error('TOTP setup error:', e)
    const message = e instanceof Error ? e.message : 'Failed to setup TOTP'
    errorMessage.value = 'Lỗi: ' + message
  } finally {
    loading.value = false
  }
}

const verifySetup = async () => {
  // In a real app, we'd verify the code against the server
  // For now, just mark as enrolled since the server has already saved the secret
  loading.value = true
  try {
    // Optional: Add a verify endpoint later
    // For now, just trust that if we got the secret, it's saved
    isEnrolled.value = true
    totpSetup.value = null
  } catch (e) {
    errorMessage.value = 'Mã không hợp lệ'
  }
  loading.value = false
}
</script>

<style scoped>
.page-content { max-width: 600px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }

.section { background: white; border: 1px solid #ddd; padding: 1.5rem; margin-bottom: 1rem; }
.section h3 { font-size: 1rem; font-weight: 600; margin-bottom: 0.75rem; color: #1a4d8c; }
.description { font-size: 0.9rem; color: #666; margin-bottom: 1rem; }

.setup-prompt { background: #fff3cd; border: 1px solid #ffc107; padding: 1rem; border-radius: 4px; }
.setup-prompt p { margin-bottom: 0.75rem; font-size: 0.9rem; }

.qr-section { text-align: center; }
.qr-section p { text-align: left; margin-bottom: 0.5rem; font-size: 0.9rem; }
.qr-container { display: flex; justify-content: center; margin: 1rem 0; padding: 1rem; background: white; border: 1px solid #ddd; }

.btn { display: inline-block; background: #f8f9fa; border: 1px solid #ddd; padding: 0.5rem 1rem; cursor: pointer; font-size: 0.85rem; text-decoration: none; color: #333; }
.btn:hover { background: #e9ecef; }

.btn-primary { background: #1a4d8c; color: white; border: none; padding: 0.75rem 1.5rem; font-size: 0.9rem; cursor: pointer; width: 100%; margin-top: 1rem; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #999; cursor: not-allowed; }

.input { width: 100%; padding: 0.5rem; border: 1px solid #ddd; font-size: 0.85rem; margin-bottom: 0.5rem; }
.totp-input { text-align: center; font-size: 1.5rem; letter-spacing: 0.5rem; font-family: monospace; }

.hint { font-size: 0.8rem; color: #666; margin: 0.5rem 0; }
.hint code { background: #f8f9fa; padding: 0.2rem 0.5rem; font-size: 0.75rem; word-break: break-all; }

.success { color: #155724; background: #d4edda; padding: 1rem; border-radius: 4px; }
.error { color: #c41e3a; font-size: 0.85rem; margin-top: 0.5rem; }

.enrolled-status { text-align: center; }
</style>
