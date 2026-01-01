<template>
  <div class="register-container">
    <div class="register-card">
      <h2>Đăng ký Chứng thư số</h2>

      <div class="stepper">
        <div class="step" :class="{ active: currentStep >= 1 }">1. Tài khoản</div>
        <div class="step" :class="{ active: currentStep >= 2 }">2. Thông tin</div>
        <div class="step" :class="{ active: currentStep >= 3 }">3. Bảo mật</div>
        <div class="step" :class="{ active: currentStep >= 4 }">4. Hoàn tất</div>
      </div>

      <form @submit.prevent="handleNextStep">
        <!-- Step 1: Account Info -->
        <div v-if="currentStep === 1">
          <div class="form-group">
            <label for="username">Tên đăng nhập</label>
            <input id="username" v-model="form.username" type="text" required placeholder="nguyenvana" />
          </div>
          <div class="form-group">
            <label for="email">Email</label>
            <input id="email" v-model="form.email" type="email" required placeholder="email@example.com" />
          </div>
          <div class="form-group">
            <label for="password">Mật khẩu</label>
            <input id="password" v-model="form.password" type="password" required minlength="6" />
          </div>
        </div>

        <!-- Step 2: Certificate Info (Request) -->
        <div v-if="currentStep === 2">
          <div class="form-group">
            <label>Thuật toán (NIST PQC)</label>
            <select v-model="form.algorithm" class="select-input">
              <option value="ML-DSA-44">ML-DSA-44 (NIST Level 2)</option>
              <option value="ML-DSA-65">ML-DSA-65 (NIST Level 3)</option>
              <option value="ML-DSA-87">ML-DSA-87 (NIST Level 5)</option>
            </select>
            <small class="hint">Chọn cấp độ bảo mật phù hợp với nhu cầu.</small>
          </div>
          <div class="form-group">
            <label>Dữ liệu KYC (CCCD/Passport)</label>
            <input v-model="form.kycData" type="text" required placeholder="012345678912" />
            <small class="hint">Số định danh cá nhân để xác minh danh tính.</small>
          </div>
        </div>

        <!-- Step 3: TOTP Setup -->
        <div v-if="currentStep === 3" class="totp-step">
          <h3>Thiết lập Xác thực 2 bước</h3>
          <p>Quét mã QR bên dưới bằng ứng dụng Google Authenticator hoặc Authy.</p>
          
          <div v-if="totpData" class="qr-container">
            <qrcode-vue :value="totpData.qrUri" :size="180" level="H" />
            <div class="secret-box">
              <small>Hoặc nhập mã thủ công:</small>
              <code>{{ totpData.secret }}</code>
            </div>
          </div>
          <div v-else class="loading-totp">Đang tạo mã bảo mật...</div>
        </div>

        <!-- Step 4: Success -->
        <div v-if="currentStep === 4" class="success-view">
          <div class="success-icon">✅</div>
          <h3>Đăng ký thành công!</h3>
          <p>Tài khoản đã được tạo và yêu cầu cấp chứng thư số đã được gửi.</p>
          <p>Bạn sẽ được chuyển hướng đến trang tổng quan...</p>
        </div>

        <div v-if="error" class="error-msg">{{ error }}</div>

        <div class="actions">
          <button v-if="currentStep === 4" type="button" class="btn-submit" disabled>Đang chuyển...</button>
          
          <template v-else>
            <button type="submit" class="btn-submit" :disabled="loading">
              {{ loading ? 'Đang xử lý...' : (currentStep === 3 ? 'Đã quét & Hoàn tất' : (currentStep === 2 ? 'Đăng ký & Tiếp tục' : 'Tiếp tục')) }}
            </button>
          </template>
        </div>
      </form>

      <p v-if="currentStep === 1" class="login-link">
        Đã có tài khoản? <NuxtLink to="/login">Đăng nhập</NuxtLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import QrcodeVue from 'qrcode.vue'

/**
 * Registration Page - Multi-step registration flow
 * Uses the refactored auth composables for clean state management
 */

// Composables
const { register, login } = useAuth()
const { post } = useApi()

// State
const currentStep = ref(1)
const loading = ref(false)
const error = ref('')
const totpData = ref<any>(null)
const form = ref({
  username: '',
  email: '',
  password: '',
  algorithm: 'ML-DSA-44',
  kycData: ''
})

async function handleNextStep() {
  error.value = ''
  
  if (currentStep.value === 1) {
    if (!form.value.username || !form.value.email || !form.value.password) {
      error.value = 'Vui lòng điền đầy đủ thông tin tài khoản.'
      return
    }
    currentStep.value = 2
  } else if (currentStep.value === 2) {
    await processRegistrationAndSetup()
  } else if (currentStep.value === 3) {
    // Finish
    currentStep.value = 4
    setTimeout(() => {
      navigateTo('/dashboard')
    }, 2000)
  }
}

async function processRegistrationAndSetup() {
  loading.value = true
  try {
    // 1. Register & Login
    await register({
       username: form.value.username,
       email: form.value.email,
       password: form.value.password,
       role: 'USER'
    })
    
    // 2. Request Certificate
    try {
      await post('/certificates/request', {
        algorithm: form.value.algorithm,
        csrPem: null
      })
    } catch (e: any) { console.warn('Cert request failed', e) }
    
    // 3. Setup TOTP
    try {
       const res = await post('/credentials/totp/setup')
       totpData.value = res
       currentStep.value = 3
    } catch (e: any) {
       error.value = 'Không thể thiết lập TOTP: ' + (e.message || e)
    }
    
  } catch (e: any) {
    error.value = e.message || 'Đăng ký thất bại'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  max-width: 450px;
  margin: 2rem auto;
  padding: 0 1rem;
}

.register-card {
  background: white;
  border: 1px solid #ddd;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.register-card h2 {
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
  color: #1a4d8c;
  text-align: center;
}

.stepper {
  display: flex;
  margin-bottom: 2rem;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.5rem;
}

.step {
  font-size: 0.8rem;
  color: #999;
  font-weight: 500;
}

.step.active {
  color: #1a4d8c;
  font-weight: 600;
}

.totp-step { text-align: center; }
.qr-container { margin: 1.5rem 0; }
.secret-box { margin-top: 1rem; background: #f8f9fa; padding: 0.5rem; border-radius: 4px; }
.secret-box code { font-weight: bold; color: #333; display: block; margin-top: 0.25rem; }

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: #333;
}

.form-group input, .select-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-group input:focus, .select-input:focus {
  outline: none;
  border-color: #1a4d8c;
  box-shadow: 0 0 0 2px rgba(26, 77, 140, 0.1);
}

.hint {
  display: block;
  margin-top: 0.25rem;
  color: #666;
  font-size: 0.8rem;
}

.error-msg {
  background: #fff5f5;
  border: 1px solid #feb2b2;
  color: #c53030;
  padding: 0.75rem;
  border-radius: 4px;
  margin-bottom: 1.5rem;
  font-size: 0.9rem;
}

.success-view {
  text-align: center;
  padding: 1rem 0;
}

.success-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.btn-submit {
  width: 100%;
  padding: 0.9rem;
  background: #1a4d8c;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-submit:hover {
  background: #153d6e;
}

.btn-submit:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.login-link {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 0.9rem;
  color: #666;
}

.login-link a {
  color: #1a4d8c;
  font-weight: 500;
}
</style>
