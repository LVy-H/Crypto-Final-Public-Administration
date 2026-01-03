<template>
  <div class="admin-certs-page">
    <div class="page-header">
      <h1>Duyệt chứng chỉ</h1>
      <p class="subtitle">Phê duyệt yêu cầu cấp chứng chỉ số</p>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-value">{{ stats.pending }}</span>
        <span class="stat-label">Chờ duyệt</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ stats.active }}</span>
        <span class="stat-label">Hoạt động</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ stats.total }}</span>
        <span class="stat-label">Tổng cộng</span>
      </div>
    </div>

    <div v-if="loading" class="loading">Đang tải...</div>

    <div v-else-if="requests.length === 0" class="empty-state">
      <div class="empty-icon">✅</div>
      <h3>Không có yêu cầu chờ duyệt</h3>
    </div>

    <div v-else class="requests-list">
      <div v-for="req in requests" :key="req.id" class="request-card">
        <div class="request-header">
          <div>
            <h3>{{ req.username || 'User' }}</h3>
            <span class="request-type">{{ req.certificateType }}</span>
          </div>
          <span class="request-algo">{{ req.keyAlgorithm }}</span>
        </div>
        
        <div class="request-info">
          <p><strong>ID:</strong> {{ req.id.substring(0, 8) }}...</p>
          <p><strong>Ngày yêu cầu:</strong> {{ formatDate(req.requestedAt) }}</p>
        </div>
        
        <div class="request-actions">
          <button @click="approve(req.id)" class="btn btn-success" :disabled="processing === req.id">
            {{ processing === req.id ? 'Đang xử lý...' : '✓ Cấp chứng chỉ' }}
          </button>
          <button @click="reject(req.id)" class="btn btn-danger">
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

const { getPendingRequests, approveCertificate, rejectCertificate, getCertificateStats } = useCertificates()

const loading = ref(true)
const requests = ref([])
const processing = ref('')
const error = ref('')
const success = ref('')
const stats = reactive({
  pending: 0,
  active: 0,
  total: 0
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('vi-VN')
}

const fetchData = async () => {
  try {
    loading.value = true
    const [reqs, statData] = await Promise.all([
      getPendingRequests(),
      getCertificateStats().catch(() => ({ pending: 0, active: 0, total: 0 }))
    ])
    requests.value = reqs
    Object.assign(stats, statData)
  } catch (e) {
    error.value = 'Không thể tải dữ liệu'
  } finally {
    loading.value = false
  }
}

const approve = async (id) => {
  try {
    processing.value = id
    error.value = ''
    success.value = ''
    await approveCertificate(id)
    success.value = 'Đã cấp chứng chỉ thành công!'
    requests.value = requests.value.filter(r => r.id !== id)
    stats.pending--
    stats.active++
  } catch (e) {
    error.value = 'Cấp chứng chỉ thất bại: ' + (e.data?.message || e.message)
  } finally {
    processing.value = ''
  }
}

const reject = async (id) => {
  try {
    await rejectCertificate(id, 'Rejected by admin')
    requests.value = requests.value.filter(r => r.id !== id)
    stats.pending--
    success.value = 'Đã từ chối yêu cầu'
  } catch (e) {
    error.value = 'Từ chối thất bại'
  }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-certs-page {
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

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.25rem;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 2rem;
  font-weight: 600;
  color: #1a4d8c;
}

.stat-label {
  font-size: 0.85rem;
  color: #666;
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

.request-header h3 {
  font-size: 1rem;
  margin: 0 0 0.25rem 0;
}

.request-type {
  font-size: 0.8rem;
  background: #e8f0fe;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  color: #1a4d8c;
}

.request-algo {
  font-size: 0.8rem;
  background: #137333;
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.request-info {
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 4px;
  margin-bottom: 1rem;
  font-size: 0.85rem;
}

.request-info p {
  margin: 0.25rem 0;
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
