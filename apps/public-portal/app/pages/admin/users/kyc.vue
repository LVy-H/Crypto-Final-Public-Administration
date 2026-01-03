<template>
  <div class="admin-kyc-page">
    <div class="page-header">
      <h1>Quản lý KYC</h1>
      <p class="subtitle">Duyệt yêu cầu xác thực danh tính</p>
    </div>

    <div v-if="loading" class="loading">Đang tải...</div>

    <div v-else-if="requests.length === 0" class="empty-state">
      <div class="empty-icon">✅</div>
      <h3>Không có yêu cầu chờ duyệt</h3>
      <p>Tất cả yêu cầu KYC đã được xử lý.</p>
    </div>

    <div v-else class="requests-list">
      <div v-for="req in requests" :key="req.username" class="request-card">
        <div class="request-header">
          <div class="user-info">
            <span class="user-avatar">👤</span>
            <div>
              <h3>{{ req.username }}</h3>
              <span class="email">{{ req.email }}</span>
            </div>
          </div>
          <span class="request-date">{{ formatDate(req.requestedAt) }}</span>
        </div>
        
        <div class="request-details">
          <p><strong>Trạng thái:</strong> {{ statusLabel(req.status) }}</p>
        </div>
        
        <div class="request-actions">
          <button @click="approve(req.username)" class="btn btn-success" :disabled="processing === req.username">
            {{ processing === req.username ? 'Đang xử lý...' : '✓ Phê duyệt' }}
          </button>
          <button @click="reject(req.username)" class="btn btn-danger" :disabled="processing === req.username">
            ✕ Từ chối
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="success" class="success-message">{{ success }}</div>
  </div>
</template>

<script setup>
definePageMeta({
  middleware: 'auth',
  layout: 'admin'
})

const { getPendingRequests, approveVerification } = useKyc()

const loading = ref(true)
const requests = ref([])
const processing = ref('')
const error = ref('')
const success = ref('')

const statusLabel = (status) => {
  const labels = {
    'PENDING': 'Chờ duyệt',
    'VERIFIED': 'Đã duyệt',
    'REJECTED': 'Từ chối'
  }
  return labels[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('vi-VN')
}

const fetchRequests = async () => {
  try {
    loading.value = true
    requests.value = await getPendingRequests()
  } catch (e) {
    error.value = 'Không thể tải danh sách yêu cầu'
  } finally {
    loading.value = false
  }
}

const approve = async (username) => {
  try {
    processing.value = username
    error.value = ''
    success.value = ''
    await approveVerification(username)
    success.value = `Đã phê duyệt ${username}`
    requests.value = requests.value.filter(r => r.username !== username)
  } catch (e) {
    error.value = 'Phê duyệt thất bại: ' + (e.data?.message || e.message)
  } finally {
    processing.value = ''
  }
}

const reject = async (username) => {
  // For now, just remove from list
  // In production, call reject API
  requests.value = requests.value.filter(r => r.username !== username)
  success.value = `Đã từ chối ${username}`
}

onMounted(fetchRequests)
</script>

<style scoped>
.admin-kyc-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.page-header h1 {
  font-size: 1.5rem;
  color: #1a4d8c;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #666;
  margin-bottom: 2rem;
}

.loading {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  background: #e6f4ea;
  border-radius: 8px;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.requests-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.request-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.25rem;
}

.request-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.user-info {
  display: flex;
  gap: 0.75rem;
}

.user-avatar {
  font-size: 1.5rem;
}

.user-info h3 {
  font-size: 1rem;
  margin: 0;
}

.email {
  font-size: 0.85rem;
  color: #666;
}

.request-date {
  font-size: 0.8rem;
  color: #888;
}

.request-details {
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 4px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.request-actions {
  display: flex;
  gap: 0.75rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  border: none;
}

.btn-success { background: #34a853; color: white; }
.btn-danger { background: #ea4335; color: white; }
.btn:disabled { opacity: 0.6; }

.error-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fce8e6;
  color: #c5221f;
  border-radius: 4px;
}

.success-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #e6f4ea;
  color: #137333;
  border-radius: 4px;
}
</style>
