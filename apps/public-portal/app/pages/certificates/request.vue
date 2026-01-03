<template>
  <div class="request-cert-page">
    <div class="page-header">
      <NuxtLink to="/certificates" class="back-link">← Quay lại</NuxtLink>
      <h1>Yêu cầu chứng chỉ số mới</h1>
    </div>

    <div v-if="success" class="success-card">
      <div class="success-icon">✅</div>
      <h2>Yêu cầu đã được gửi!</h2>
      <p>Mã yêu cầu: <strong>{{ requestId }}</strong></p>
      <p>Yêu cầu của bạn đang được xem xét. Bạn sẽ nhận được thông báo khi được phê duyệt.</p>
      <NuxtLink to="/certificates" class="btn btn-primary">Xem danh sách chứng chỉ</NuxtLink>
    </div>

    <form v-else @submit.prevent="submitRequest" class="request-form">
      <div class="form-section">
        <h3>Loại chứng chỉ</h3>
        <div class="radio-group">
          <label class="radio-option" :class="{ active: form.certificateType === 'SIGNATURE' }">
            <input type="radio" v-model="form.certificateType" value="SIGNATURE" />
            <div class="option-content">
              <strong>Chữ ký số</strong>
              <span>Dùng để ký tài liệu điện tử</span>
            </div>
          </label>
          <label class="radio-option" :class="{ active: form.certificateType === 'ENCRYPTION' }">
            <input type="radio" v-model="form.certificateType" value="ENCRYPTION" />
            <div class="option-content">
              <strong>Mã hóa</strong>
              <span>Dùng để mã hóa dữ liệu</span>
            </div>
          </label>
        </div>
      </div>

      <div class="form-section">
        <h3>Thuật toán mã hóa</h3>
        <div class="radio-group">
          <label class="radio-option" :class="{ active: form.keyAlgorithm === 'ML-DSA-65' }">
            <input type="radio" v-model="form.keyAlgorithm" value="ML-DSA-65" />
            <div class="option-content">
              <strong>ML-DSA-65</strong>
              <span>Post-Quantum (FIPS 204) - Khuyến nghị</span>
            </div>
          </label>
          <label class="radio-option" :class="{ active: form.keyAlgorithm === 'ML-DSA-87' }">
            <input type="radio" v-model="form.keyAlgorithm" value="ML-DSA-87" />
            <div class="option-content">
              <strong>ML-DSA-87</strong>
              <span>Post-Quantum (FIPS 204) - Bảo mật cao nhất</span>
            </div>
          </label>
          <label class="radio-option" :class="{ active: form.keyAlgorithm === 'ML-DSA-44' }">
            <input type="radio" v-model="form.keyAlgorithm" value="ML-DSA-44" />
            <div class="option-content">
              <strong>ML-DSA-44</strong>
              <span>Post-Quantum (FIPS 204) - Nhanh nhất</span>
            </div>
          </label>
        </div>
      </div>

      <div class="info-box">
        <strong>💡 Lưu ý:</strong>
        <p>Chứng chỉ sử dụng thuật toán ML-DSA (Dilithium) theo chuẩn NIST FIPS 204, chống lại máy tính lượng tử.</p>
      </div>

      <button type="submit" class="btn btn-primary btn-large" :disabled="submitting">
        {{ submitting ? 'Đang xử lý...' : 'Gửi yêu cầu' }}
      </button>

      <div v-if="error" class="error-message">{{ error }}</div>
    </form>
  </div>
</template>

<script setup>
definePageMeta({
  middleware: 'auth'
})

const { requestCertificate } = useCertificates()

const submitting = ref(false)
const success = ref(false)
const requestId = ref('')
const error = ref('')

const form = reactive({
  certificateType: 'SIGNATURE',
  keyAlgorithm: 'ML-DSA-65'
})

const submitRequest = async () => {
  try {
    submitting.value = true
    error.value = ''
    const result = await requestCertificate(form.certificateType, form.keyAlgorithm)
    requestId.value = result.id
    success.value = true
  } catch (e) {
    error.value = e.data?.message || 'Gửi yêu cầu thất bại. Vui lòng thử lại.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.request-cert-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.back-link {
  color: #1a4d8c;
  text-decoration: none;
  font-size: 0.9rem;
}

.page-header h1 {
  font-size: 1.5rem;
  color: #1a4d8c;
  margin-top: 1rem;
  margin-bottom: 2rem;
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

.success-card p {
  color: #333;
  margin-bottom: 0.5rem;
}

.request-form {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
}

.form-section {
  margin-bottom: 2rem;
}

.form-section h3 {
  font-size: 1rem;
  color: #333;
  margin-bottom: 1rem;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.radio-option {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.radio-option:hover {
  border-color: #1a4d8c;
}

.radio-option.active {
  border-color: #1a4d8c;
  background: #f0f7ff;
}

.radio-option input {
  margin-top: 0.2rem;
}

.option-content {
  display: flex;
  flex-direction: column;
}

.option-content strong {
  font-size: 0.95rem;
  color: #333;
}

.option-content span {
  font-size: 0.8rem;
  color: #666;
  margin-top: 0.25rem;
}

.info-box {
  background: #e8f0fe;
  padding: 1rem;
  border-radius: 6px;
  margin-bottom: 1.5rem;
  font-size: 0.85rem;
}

.info-box strong {
  display: block;
  margin-bottom: 0.5rem;
  color: #1a4d8c;
}

.info-box p {
  color: #333;
  margin: 0;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  text-decoration: none;
  border: none;
  display: inline-block;
}

.btn-primary { background: #1a4d8c; color: white; }
.btn-primary:disabled { background: #ccc; }
.btn-large { width: 100%; padding: 1rem; font-size: 1rem; }

.error-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fce8e6;
  color: #c5221f;
  border-radius: 4px;
  font-size: 0.9rem;
}
</style>
