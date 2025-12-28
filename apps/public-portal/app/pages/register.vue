<template>

    <div class="register-container">
      <div class="register-card">
        <h2>Đăng ký</h2>

        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label for="username">Tên đăng nhập</label>
            <input 
              id="username"
              v-model="form.username" 
              type="text" 
              required
            />
          </div>

          <div class="form-group">
            <label for="email">Email</label>
            <input 
              id="email"
              v-model="form.email" 
              type="email" 
              required
            />
          </div>

          <div class="form-group">
            <label for="password">Mật khẩu</label>
            <input 
              id="password"
              v-model="form.password" 
              type="password" 
              required
              minlength="6"
            />
          </div>

          <div v-if="error" class="error-msg">{{ error }}</div>
          <div v-if="success" class="success-msg">Đăng ký thành công! Đang chuyển hướng...</div>

          <button type="submit" class="btn-submit" :disabled="loading">
            {{ loading ? 'Đang xử lý...' : 'Đăng ký' }}
          </button>
        </form>

        <p class="login-link">
          Đã có tài khoản? <NuxtLink to="/login">Đăng nhập</NuxtLink>
        </p>
      </div>
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
    const apiBase = config.public.apiBase || 'http://localhost:8080/api/v1'
    const res = await fetch(`${apiBase}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })

    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      throw new Error(data.message || 'Đăng ký thất bại')
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
.register-container {
  max-width: 400px;
  margin: 2rem auto;
  padding: 0 1rem;
}

.register-card {
  background: white;
  border: 1px solid #ddd;
  padding: 2rem;
}

.register-card h2 {
  font-size: 1.25rem;
  margin-bottom: 1.5rem;
  color: #1a4d8c;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid #1a4d8c;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 1px solid #ccc;
  font-size: 0.9rem;
}

.form-group input:focus {
  outline: none;
  border-color: #1a4d8c;
}

.error-msg {
  background: #fee;
  border: 1px solid #c41e3a;
  color: #c41e3a;
  padding: 0.6rem;
  margin-bottom: 1rem;
  font-size: 0.85rem;
}

.success-msg {
  background: #d4edda;
  border: 1px solid #28a745;
  color: #155724;
  padding: 0.6rem;
  margin-bottom: 1rem;
  font-size: 0.85rem;
}

.btn-submit {
  width: 100%;
  padding: 0.75rem;
  background: #1a4d8c;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-submit:hover {
  background: #153d6e;
}

.btn-submit:disabled {
  background: #999;
  cursor: not-allowed;
}

.login-link {
  margin-top: 1rem;
  text-align: center;
  font-size: 0.85rem;
  color: #666;
}

.login-link a {
  color: #1a4d8c;
}
</style>
