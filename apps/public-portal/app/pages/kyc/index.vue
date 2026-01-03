<template>
  <div class="kyc-page">
    <div class="page-header">
      <h1>Xác thực danh tính (KYC)</h1>
      <p class="subtitle">Xác thực tài khoản để sử dụng chữ ký số</p>
    </div>

    <!-- Status Display -->
    <div class="status-card" :class="statusClass">
      <div class="status-icon">
        <span v-if="loading">⏳</span>
        <span v-else-if="status === 'VERIFIED'">✅</span>
        <span v-else-if="status === 'PENDING'">🕐</span>
        <span v-else-if="status === 'REJECTED'">❌</span>
        <span v-else>📋</span>
      </div>
      <div class="status-content">
        <h2>{{ statusTitle }}</h2>
        <p>{{ statusMessage }}</p>
      </div>
    </div>

    <!-- Action Section -->
    <div v-if="status === 'UNVERIFIED'" class="action-section">
      <h3>Gửi yêu cầu xác thực</h3>
      <p>Để sử dụng chữ ký số, bạn cần được xác thực danh tính.</p>
      
      <form @submit.prevent="submitRequest" class="kyc-form">
        <div class="form-group">
          <label>Họ và tên đầy đủ</label>
          <input v-model="form.fullName" type="text" required placeholder="Nguyễn Văn A" />
        </div>
        
        <div class="form-group">
          <label>Số CCCD/CMND</label>
          <input v-model="form.idNumber" type="text" required placeholder="001234567890" />
        </div>
        
        <div class="form-group">
          <label>Số điện thoại</label>
          <input v-model="form.phone" type="tel" placeholder="0901234567" />
        </div>
        
        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu xác thực' }}
        </button>
      </form>
    </div>

    <!-- Verified Actions -->
    <div v-if="status === 'VERIFIED'" class="verified-actions">
      <h3>Tài khoản đã được xác thực</h3>
      <div class="action-buttons">
        <NuxtLink to="/certificates" class="btn btn-primary">Quản lý chứng chỉ số</NuxtLink>
        <NuxtLink to="/sign" class="btn btn-secondary">Ký tài liệu</NuxtLink>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
definePageMeta({
  middleware: 'auth'
})

const { getMyStatus, submitVerificationRequest } = useKyc()

const loading = ref(true)
const submitting = ref(false)
const status = ref('UNVERIFIED')
const error = ref('')

const form = reactive({
  fullName: '',
  idNumber: '',
  phone: ''
})

const statusClass = computed(() => ({
  'status-verified': status.value === 'VERIFIED',
  'status-pending': status.value === 'PENDING',
  'status-rejected': status.value === 'REJECTED',
  'status-unverified': status.value === 'UNVERIFIED'
}))

const statusTitle = computed(() => {
  switch (status.value) {
    case 'VERIFIED': return 'Đã xác thực'
    case 'PENDING': return 'Đang chờ xét duyệt'
    case 'REJECTED': return 'Yêu cầu bị từ chối'
    default: return 'Chưa xác thực'
  }
})

const statusMessage = computed(() => {
  switch (status.value) {
    case 'VERIFIED': return 'Tài khoản của bạn đã được xác thực. Bạn có thể sử dụng đầy đủ các tính năng.'
    case 'PENDING': return 'Yêu cầu của bạn đang được xem xét. Vui lòng chờ trong 1-2 ngày làm việc.'
    case 'REJECTED': return 'Yêu cầu xác thực bị từ chối. Vui lòng kiểm tra lại thông tin và gửi lại.'
    default: return 'Vui lòng hoàn tất xác thực danh tính để sử dụng chữ ký số.'
  }
})

const fetchStatus = async () => {
  try {
    loading.value = true
    const result = await getMyStatus()
    status.value = result.status
  } catch (e) {
    error.value = 'Không thể tải trạng thái xác thực'
  } finally {
    loading.value = false
  }
}

const submitRequest = async () => {
  try {
    submitting.value = true
    error.value = ''
    await submitVerificationRequest(form)
    status.value = 'PENDING'
  } catch (e) {
    error.value = e.data?.message || 'Gửi yêu cầu thất bại'
  } finally {
    submitting.value = false
  }
}

onMounted(fetchStatus)
</script>

<style scoped>
.kyc-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 1.5rem;
  color: #1a4d8c;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #666;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 2rem;
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
}

.status-verified { background: #e6f4ea; border-color: #34a853; }
.status-pending { background: #fef7e0; border-color: #f9ab00; }
.status-rejected { background: #fce8e6; border-color: #ea4335; }

.status-icon {
  font-size: 2rem;
}

.status-content h2 {
  font-size: 1.1rem;
  margin-bottom: 0.25rem;
}

.status-content p {
  color: #666;
  font-size: 0.9rem;
}

.action-section, .verified-actions {
  background: white;
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.action-section h3, .verified-actions h3 {
  margin-bottom: 1rem;
  font-size: 1.1rem;
}

.kyc-form {
  margin-top: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.form-group input:focus {
  outline: none;
  border-color: #1a4d8c;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  text-decoration: none;
  display: inline-block;
}

.btn-primary {
  background: #1a4d8c;
  color: white;
}

.btn-primary:disabled {
  background: #ccc;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.error-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fce8e6;
  color: #c5221f;
  border-radius: 4px;
  font-size: 0.9rem;
}
</style>
