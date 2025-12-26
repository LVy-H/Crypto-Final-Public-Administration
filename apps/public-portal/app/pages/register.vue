<template>
  <div class="auth-page">
    <nav class="navbar glass">
      <div class="container flex items-center justify-between">
        <NuxtLink to="/" class="nav-brand">
          <span class="brand-icon">🔐</span>
          <span class="brand-text">PQC Digital Signature</span>
        </NuxtLink>
      </div>
    </nav>

    <div class="container auth-container">
      <div class="auth-card card fade-in">
        <div class="auth-header text-center">
          <h2>Create Account</h2>
          <p class="text-muted">Get your post-quantum ID</p>
        </div>

        <form @submit.prevent="handleRegister" class="auth-form">
          <div class="form-group">
            <label>Username</label>
            <input v-model="form.username" type="text" required placeholder="johndoe" class="input" />
          </div>

          <div class="form-group">
            <label>Email Address</label>
            <input v-model="form.email" type="email" required placeholder="john@example.com" class="input" />
          </div>

          <div class="form-group">
            <label>Signature Algorithm</label>
            <select v-model="form.algorithm" class="input">
              <option value="Dilithium2">ML-DSA-44 (Dilithium2)</option>
              <option value="Dilithium3">ML-DSA-65 (Dilithium3)</option>
              <option value="Falcon-512">Falcon-512 (Fast Verify)</option>
            </select>
            <p class="text-xs text-muted mt-1">Select the NIST standardized algorithm for your keys.</p>
          </div>

          <div class="form-group">
            <label>National ID (KYC)</label>
            <input v-model="form.kycData.nationalId" type="text" required class="input" />
          </div>

          <div v-if="error" class="alert alert-error">
            {{ error }}
          </div>

          <div v-if="success" class="alert alert-success">
            Registration successful! Redirecting...
          </div>

          <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
            {{ loading ? 'Processing...' : 'Register' }}
          </button>

          <div class="text-center mt-4">
            <p class="text-sm">Already have an account? <NuxtLink to="/login" class="text-primary">Sign In</NuxtLink></p>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
const config = useRuntimeConfig()
const router = useRouter()

const form = ref({
  username: '',
  email: '',
  algorithm: 'Dilithium2',
  kycData: {
    nationalId: ''
  }
})

const loading = ref(false)
const error = ref('')
const success = ref(false)

async function handleRegister() {
  loading.value = true
  error.value = ''
  
  try {
    const { data, error: apiError } = await useFetch(`${config.public.apiBase}/ra/request`, {
      method: 'POST',
      body: form.value
    })

    if (apiError.value) {
      throw new Error(apiError.value.message || 'Registration failed')
    }

    success.value = true
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: var(--bg-dark);
  padding-top: 5rem;
}

.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2rem 0;
}

.auth-card {
  width: 100%;
  max-width: 480px;
  background: var(--bg-card);
  padding: 2.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.auth-header {
  margin-bottom: 2rem;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.text-xs {
  font-size: 0.75rem;
}

.mt-1 {
  margin-top: 0.25rem;
}

.mt-4 {
  margin-top: 1rem;
}

.alert {
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
}

.alert-error {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.alert-success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.2);
}
</style>
