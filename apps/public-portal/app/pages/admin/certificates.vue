<template>

    <div class="admin-page">
      <h2 class="page-title">Quản lý chứng chỉ</h2>

      <div v-if="loading" class="loading">Đang tải...</div>

      <template v-else>
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">Tổng số</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.active }}</div>
            <div class="stat-label">Hoạt động</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.revoked }}</div>
            <div class="stat-label">Thu hồi</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.expiring }}</div>
            <div class="stat-label">Sắp hết hạn</div>
          </div>
        </div>

        <div class="section">
          <h3>Danh sách chứng chỉ</h3>
          <table class="data-table">
            <thead>
              <tr>
                <th>Serial</th>
                <th>Chủ thể</th>
                <th>Thuật toán</th>
                <th>Hết hạn</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="certs.length === 0">
                <td colspan="6" class="text-center">Không có dữ liệu</td>
              </tr>
              <tr v-for="cert in certs" :key="cert.serialNumber">
                <td class="mono">{{ cert.serialNumber?.substring(0, 8) }}</td>
                <td>{{ cert.subjectDN || cert.username }}</td>
                <td>{{ cert.algorithm || 'ML-DSA' }}</td>
                <td>{{ formatDate(cert.notAfter) }}</td>
                <td><span :class="['badge', 'badge-' + getStatus(cert)]">{{ getStatusText(cert) }}</span></td>
                <td>
                  <button class="btn btn-sm">Chi tiết</button>
                  <button v-if="!cert.revoked" @click="revokeCert(cert.serialNumber)" class="btn btn-sm btn-danger">Thu hồi</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>

</template>

<script setup>
definePageMeta({
  middleware: 'auth'
})

const config = useRuntimeConfig()
const { token } = useAuth()

const loading = ref(true)
const stats = ref({ total: 0, active: 0, revoked: 0, expiring: 0 })
const certs = ref([])

const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A'
  return new Date(dateStr).toLocaleDateString('vi-VN')
}

const getStatus = (cert) => {
  if (cert.revoked) return 'revoked'
  const expiry = new Date(cert.notAfter)
  const now = new Date()
  const daysLeft = (expiry - now) / (1000 * 60 * 60 * 24)
  if (daysLeft < 30) return 'expiring'
  return 'active'
}

const getStatusText = (cert) => {
  const status = getStatus(cert)
  return { active: 'Hoạt động', expiring: 'Sắp hết hạn', revoked: 'Thu hồi' }[status]
}

const loadData = async () => {
  try {
    loading.value = true
    const authToken = token.value || localStorage.getItem('token')
    const headers = { 'Authorization': `Bearer ${authToken}` }
    
    // Load stats
    try {
      const res = await fetch(`${apiBase.value}/admin/certificates/stats`, { headers })
      if (res.ok) stats.value = await res.json()
    } catch (e) { console.warn('Cert stats not available') }
    
    // Load certificates
    try {
      const res = await fetch(`${apiBase.value}/admin/certificates`, { headers })
      if (res.ok) certs.value = await res.json()
    } catch (e) { console.warn('Certs not available') }
    
  } catch (e) {
    console.error('Error loading certificates:', e)
  } finally {
    loading.value = false
  }
}

const revokeCert = async (serial) => {
  if (!confirm('Xác nhận thu hồi chứng chỉ?')) return
  
  try {
    const authToken = token.value || localStorage.getItem('token')
    const res = await fetch(`${apiBase.value}/certificates/${serial}/revoke`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    
    if (res.ok) {
      alert('Đã thu hồi')
      loadData()
    } else {
      alert('Lỗi khi thu hồi')
    }
  } catch (e) {
    alert('Lỗi kết nối')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-page { max-width: 1000px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }
.loading { padding: 2rem; text-align: center; color: #666; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.5rem; }
.stat-card { background: white; border: 1px solid #ddd; padding: 1rem; text-align: center; }
.stat-value { font-size: 1.5rem; font-weight: 700; color: #1a4d8c; }
.stat-label { font-size: 0.8rem; color: #666; }

.section { background: white; border: 1px solid #ddd; padding: 1.25rem; }
.section h3 { font-size: 0.95rem; margin-bottom: 1rem; padding-bottom: 0.5rem; border-bottom: 1px solid #eee; }

.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.mono { font-family: monospace; font-size: 0.8rem; }

.btn { padding: 0.25rem 0.5rem; border: 1px solid #ddd; background: white; cursor: pointer; font-size: 0.75rem; margin-right: 0.25rem; }
.btn-danger { background: #dc3545; color: white; border-color: #dc3545; }

.badge { padding: 0.2rem 0.5rem; font-size: 0.7rem; border-radius: 3px; }
.badge-active { background: #d4edda; color: #155724; }
.badge-expiring { background: #ffc107; color: #333; }
.badge-revoked { background: #f8d7da; color: #721c24; }

.text-center { text-align: center; color: #999; }
</style>
