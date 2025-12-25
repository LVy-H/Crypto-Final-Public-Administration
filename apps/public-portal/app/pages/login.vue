<template>
  <div class="login-page">
    <header class="gov-header">
      <div class="header-content">
        <span class="logo">🏛️</span>
        <h1>Hệ thống Chữ ký số Lượng tử</h1>
      </div>
    </header>

    <main class="login-main">
      <div class="login-card">
        <h2>Đăng nhập hệ thống</h2>
        
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label for="username">Tên đăng nhập</label>
            <input 
              id="username"
              v-model="form.username" 
              type="text" 
              placeholder="Nhập tên đăng nhập"
              required
            />
          </div>

          <div class="form-group">
            <label for="password">Mật khẩu</label>
            <input 
              id="password"
              v-model="form.password" 
              type="password" 
              placeholder="Nhập mật khẩu"
              required
            />
          </div>

          <div v-if="error" class="error-message">
            {{ error }}
          </div>

          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? 'Đang xử lý...' : 'Đăng nhập' }}
          </button>
        </form>

        <div class="login-footer">
          <p>Chưa có tài khoản? <NuxtLink to="/register">Đăng ký tài khoản mới</NuxtLink></p>
        </div>
      </div>

      <div class="info-box">
        <h3>Thông tin hệ thống</h3>
        <ul>
          <li>Hệ thống sử dụng thuật toán ML-DSA (Dilithium) đạt chuẩn NIST</li>
          <li>Tuân thủ Nghị định 130/2018/NĐ-CP về chữ ký số</li>
          <li>Bảo mật an toàn trước máy tính lượng tử</li>
        </ul>
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

const form = ref({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  loading.value = true
  error.value = ''
  
  try {
    const response = await $fetch(`${config.public.apiBase}/auth/login`, {
      method: 'POST',
      body: form.value
    })
    
    if (response.token) {
      localStorage.setItem('token', response.token)
      localStorage.setItem('user', JSON.stringify(response.user || { username: form.value.username }))
      router.push('/dashboard')
    }
  } catch (e) {
    error.value = 'Đăng nhập thất bại. Vui lòng kiểm tra tên đăng nhập và mật khẩu.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.gov-header {
  background: #1a4d8c;
  color: white;
  padding: 16px 24px;
  border-bottom: 3px solid #c41e3a;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 1200px;
  margin: 0 auto;
}

.logo { font-size: 28px; }

.gov-header h1 {
  font-size: 20px;
  font-weight: 600;
}

.login-main {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 32px;
  padding: 48px 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.login-card {
  background: white;
  border: 1px solid #ddd;
  padding: 32px;
  width: 400px;
}

.login-card h2 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #1a4d8c;
  color: #1a4d8c;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ccc;
  font-size: 14px;
}

.form-group input:focus {
  outline: none;
  border-color: #1a4d8c;
}

.error-message {
  background: #fee;
  border: 1px solid #c41e3a;
  color: #c41e3a;
  padding: 10px 12px;
  margin-bottom: 16px;
  font-size: 13px;
}

.btn {
  width: 100%;
  padding: 12px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-primary {
  background: #1a4d8c;
  color: white;
}

.btn-primary:hover {
  background: #153d6e;
}

.btn-primary:disabled {
  background: #999;
  cursor: not-allowed;
}

.login-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #666;
}

.login-footer a {
  color: #1a4d8c;
}

.info-box {
  background: white;
  border: 1px solid #ddd;
  padding: 24px;
  width: 320px;
}

.info-box h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #1a4d8c;
}

.info-box ul {
  list-style: none;
  padding: 0;
}

.info-box li {
  padding: 8px 0;
  padding-left: 20px;
  position: relative;
  font-size: 13px;
  color: #555;
  border-bottom: 1px solid #eee;
}

.info-box li:before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #28a745;
}

.gov-footer {
  background: #333;
  color: #ccc;
  text-align: center;
  padding: 16px;
  font-size: 13px;
}
</style>
