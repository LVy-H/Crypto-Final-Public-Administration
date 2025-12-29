<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = ref({ username: '', email: '', algorithm: 'ML-DSA-44', kycData: '' })
const loading = ref(false)
const error = ref('')
const success = ref(false)

async function handleRegister() {
  loading.value = true
  error.value = ''
  
  // Mock API Call
  // POST /api/v1/ra/request
  setTimeout(() => {
    loading.value = false
    if (form.value.username && form.value.email) {
      success.value = true
      setTimeout(() => router.push('/dashboard'), 1500)
    } else {
      error.value = 'Vui lòng điền đầy đủ thông tin'
    }
  }, 1000)
}
</script>

<template>
  <div class="page-container">
    <div class="card">
      <h2 class="title">Đăng ký CKS (RA Request)</h2>
      <form @submit.prevent="handleRegister">
        
        <div class="form-group">
          <label>Tên đăng nhập / Username</label>
          <input v-model="form.username" type="text" required placeholder="nguyenvana" />
        </div>

        <div class="form-group">
          <label>Email</label>
          <input v-model="form.email" type="email" required placeholder="email@example.com" />
        </div>

        <div class="form-group">
          <label>Thuật toán (NIST PQC)</label>
          <select v-model="form.algorithm">
            <option value="ML-DSA-44">ML-DSA-44 (NIST Level 2)</option>
            <option value="ML-DSA-65">ML-DSA-65 (NIST Level 3)</option>
            <option value="ML-DSA-87">ML-DSA-87 (NIST Level 5)</option>
          </select>
        </div>

        <div class="form-group">
          <label>Dữ liệu KYC (CCCD/Passport)</label>
          <input v-model="form.kycData" type="text" required placeholder="012345678912" />
        </div>

        <div v-if="error" class="error">{{ error }}</div>
        <div v-if="success" class="success">Gửi yêu cầu thành công!</div>

        <button type="submit" :disabled="loading" class="btn-primary">
          {{ loading ? 'Đang xử lý...' : 'Gửi yêu cầu' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.page-container { display: flex; justify-content: center; padding: 2rem; background: #f0f2f5; min-height: 90vh; }
.card { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); width: 100%; max-width: 450px; }
.title { color: #1a4d8c; margin-bottom: 1.5rem; text-align: center; border-bottom: 1px solid #eee; padding-bottom: 1rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.5rem; font-weight: 500; font-size: 0.9rem; color: #333; }
.form-group input, .form-group select { width: 100%; padding: 0.6rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; box-sizing: border-box; }
.form-group input:focus, .form-group select:focus { border-color: #1a4d8c; outline: none; }
.btn-primary { width: 100%; padding: 0.75rem; background: #1a4d8c; color: white; border: none; border-radius: 4px; font-size: 1rem; cursor: pointer; font-weight: 600; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }
.error { color: #dc3545; background: #f8d7da; padding: 0.5rem; margin-bottom: 1rem; border-radius: 4px; font-size: 0.9rem; }
.success { color: #155724; background: #d4edda; padding: 0.5rem; margin-bottom: 1rem; border-radius: 4px; font-size: 0.9rem; }
</style>
