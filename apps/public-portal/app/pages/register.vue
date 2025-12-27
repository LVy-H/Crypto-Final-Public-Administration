<template>
  <div class="page">
    <header class="gov-header">
      <div class="header-content">
        <div class="header-left">
          <span class="logo">🏛️</span>
          <h1>Hệ thống Chữ ký số Lượng tử</h1>
        </div>
        <div class="header-right">
          <NuxtLink to="/" class="btn-link">Trang chủ</NuxtLink>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="auth-container">
        <div class="auth-box">
          <h2>Tạo tài khoản</h2>
          <p class="subtitle">Đăng ký để nhận chứng chỉ số lượng tử</p>

          <form @submit.prevent="handleRegister">
            <div class="form-group">
              <label>Tên đăng nhập</label>
              <input v-model="form.username" type="text" required placeholder="nguyenvana" />
            </div>

            <div class="form-group">
              <label>Email</label>
              <input v-model="form.email" type="email" required placeholder="email@example.com" />
            </div>

            <div class="form-group">
              <label>Mật khẩu</label>
              <input v-model="form.password" type="password" required placeholder="••••••••" minlength="6" />
            </div>

            <div v-if="error" class="alert error">{{ error }}</div>
            <div v-if="success" class="alert success">Đăng ký thành công! Đang chuyển hướng...</div>

            <button type="submit" class="btn-primary" :disabled="loading">
              {{ loading ? 'Đang xử lý...' : 'Đăng ký' }}
            </button>

            <p class="link-text">Đã có tài khoản? <NuxtLink to="/login">Đăng nhập</NuxtLink></p>
          </form>
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
const router = useRouter()

const form = ref({ username: '', email: '', password: '' })
const loading = ref(false)
const error = ref('')
const success = ref(false)

async function handleRegister() {
  loading.value = true
  error.value = ''
  
  try {
    const { data, error: apiError } = await useFetch(`${config.public.apiBase}/auth/register`, {
      method: 'POST',
      body: form.value
    })

    if (apiError.value) {
      throw new Error(apiError.value.message || 'Đăng ký thất bại')
    }

    success.value = true
    setTimeout(() => router.push('/login'), 2000)
  } catch (e) {
    error.value = e.message
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
.btn-link { color: white; text-decoration: underline; }
.main-content { flex: 1; display: flex; align-items: center; justify-content: center; padding: 24px; }
.auth-container { width: 100%; max-width: 400px; }
.auth-box { background: white; border: 1px solid #ddd; padding: 32px; }
.auth-box h2 { font-size: 20px; font-weight: 600; color: #1a4d8c; margin-bottom: 4px; }
.subtitle { color: #666; font-size: 14px; margin-bottom: 24px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; font-weight: 500; margin-bottom: 6px; color: #333; }
.form-group input { width: 100%; padding: 10px 12px; border: 1px solid #ddd; font-size: 14px; }
.form-group input:focus { border-color: #1a4d8c; outline: none; }
.alert { padding: 10px 12px; margin-bottom: 16px; font-size: 13px; }
.alert.error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.alert.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.btn-primary { width: 100%; background: #1a4d8c; color: white; border: none; padding: 12px; font-size: 14px; cursor: pointer; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }
.link-text { text-align: center; margin-top: 16px; font-size: 13px; color: #666; }
.link-text a { color: #1a4d8c; }
.gov-footer { background: #333; color: #ccc; text-align: center; padding: 16px; font-size: 13px; }
</style>
