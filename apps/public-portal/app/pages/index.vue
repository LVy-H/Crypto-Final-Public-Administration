<template>

    <div class="landing">
      <div class="hero">
        <h1>Hệ thống Chữ ký số</h1>
        <p class="subtitle">Ký số và xác thực văn bản điện tử</p>
        <div class="actions">
          <NuxtLink to="/register" class="btn btn-primary">Đăng ký</NuxtLink>
          <NuxtLink to="/login" class="btn btn-secondary">Đăng nhập</NuxtLink>
        </div>
      </div>

      <div class="info-section">
        <div class="info-card">
          <h3>Thuật toán</h3>
          <p>ML-DSA (Dilithium) - NIST PQC</p>
        </div>
        <div class="info-card">
          <h3>Tiêu chuẩn</h3>
          <p>Nghị định 130/2018/NĐ-CP</p>
        </div>
        <div class="info-card">
          <h3>Bảo mật</h3>
          <p>Post-Quantum Cryptography</p>
        </div>
      </div>
    </div>

</template>

<script setup>
const { isAuthenticated } = useAuth()
const router = useRouter()

onMounted(() => {
  if (isAuthenticated.value) {
    router.push('/dashboard')
  }
})
</script>

<style scoped>
.landing {
  max-width: 800px;
  margin: 0 auto;
  padding: 3rem 1.5rem;
}

.hero {
  text-align: center;
  padding: 3rem 0;
}

.hero h1 {
  font-size: 2rem;
  color: #1a4d8c;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #666;
  margin-bottom: 2rem;
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn {
  padding: 0.75rem 1.5rem;
  text-decoration: none;
  border-radius: 4px;
  font-size: 0.9rem;
}

.btn-primary {
  background: #1a4d8c;
  color: white;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.info-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-top: 3rem;
}

.info-card {
  background: white;
  padding: 1.25rem;
  border: 1px solid #ddd;
  text-align: center;
}

.info-card h3 {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.info-card p {
  font-size: 0.9rem;
  color: #333;
  font-weight: 500;
}
</style>
