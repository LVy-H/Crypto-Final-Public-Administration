<template>

    <div class="login-container">
      <div class="login-card">
        <h2>Đăng nhập</h2>
        
        <form @submit.prevent="handleLogin">
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
            <label for="password">Mật khẩu</label>
            <input 
              id="password"
              v-model="form.password" 
              type="password" 
              required
            />
          </div>

          <div v-if="error" class="error-msg">{{ error }}</div>

          <button type="submit" class="btn-submit" :disabled="loading">
            {{ loading ? 'Đang xử lý...' : 'Đăng nhập' }}
          </button>
        </form>

        <p class="register-link">
          Chưa có tài khoản? <NuxtLink to="/register">Đăng ký</NuxtLink>
        </p>
      </div>
    </div>

</template>

<script setup>
const router = useRouter()
const { login, checkAuth, hasAdminAccess } = useAuth()

const form = ref({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (checkAuth()) {
    router.push('/dashboard')
  }
})

const handleLogin = async () => {
  loading.value = true
  error.value = ''
  
  try {
    await login(form.value.username, form.value.password)
    
    // Redirect based on role
    if (hasAdminAccess.value) {
      router.push('/admin')
    } else {
      router.push('/dashboard')
    }
  } catch (e) {
    error.value = 'Đăng nhập thất bại. Vui lòng kiểm tra thông tin.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  max-width: 400px;
  margin: 2rem auto;
  padding: 0 1rem;
}

.login-card {
  background: white;
  border: 1px solid #ddd;
  padding: 2rem;
}

.login-card h2 {
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

.register-link {
  margin-top: 1rem;
  text-align: center;
  font-size: 0.85rem;
  color: #666;
}

.register-link a {
  color: #1a4d8c;
}
</style>
