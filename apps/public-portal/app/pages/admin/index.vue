<template>

    <div class="admin-page">
      <h2 class="page-title">Quản trị hệ thống</h2>

      <div v-if="loading" class="loading">Đang tải...</div>

      <template v-else>
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">Người dùng</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.activeCerts }}</div>
            <div class="stat-label">Chứng chỉ</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.pendingRequests }}</div>
            <div class="stat-label">Chờ duyệt</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.todaySignatures }}</div>
            <div class="stat-label">Ký hôm nay</div>
          </div>
        </div>

        <div class="section">
          <h3>Yêu cầu chờ xử lý</h3>
          <table class="data-table">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Người dùng</th>
                <th>Loại</th>
                <th>Ngày</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="pendingRequests.length === 0">
                <td colspan="5" class="text-center">Không có yêu cầu</td>
              </tr>
              <tr v-for="req in pendingRequests" :key="req.id">
                <td>{{ req.id }}</td>
                <td>{{ req.username }}</td>
                <td>{{ req.type }}</td>
                <td>{{ formatDate(req.createdAt) }}</td>
                <td>
                  <button @click="approve(req.id)" class="btn btn-primary btn-sm">Duyệt</button>
                  <button @click="reject(req.id)" class="btn btn-secondary btn-sm">Từ chối</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="section">
          <h3>Trạng thái dịch vụ</h3>
          <div class="service-grid">
            <div v-for="svc in services" :key="svc.name" class="service-item">
              <span class="status-dot" :class="svc.status"></span>
              <span>{{ svc.name }}</span>
            </div>
          </div>
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
const stats = ref({ totalUsers: 0, activeCerts: 0, pendingRequests: 0, todaySignatures: 0 })
const pendingRequests = ref([])
const services = ref([])

const apiBase = computed(() => config.public.apiBase || '/api/v1')

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('vi-VN')
}

const loadData = async () => {
  try {
    loading.value = true
    const authToken = token.value || localStorage.getItem('token')
    const headers = { 'Authorization': `Bearer ${authToken}` }
    
    // Load admin stats
    try {
      const res = await fetch(`${apiBase.value}/admin/stats`, { headers })
      if (res.ok) stats.value = await res.json()
    } catch (e) { console.warn('Admin stats not available') }
    
    // Load pending requests
    try {
      const res = await fetch(`${apiBase.value}/admin/requests/pending`, { headers })
      if (res.ok) pendingRequests.value = await res.json()
    } catch (e) { console.warn('Pending requests not available') }
    
    // Load service health
    try {
      const res = await fetch(`${apiBase.value}/health/services`, { headers })
      if (res.ok) {
        services.value = await res.json()
      } else {
        // Default services list
        services.value = [
          { name: 'API Gateway', status: 'online' },
          { name: 'Identity Service', status: 'online' },
          { name: 'Cloud Sign', status: 'online' },
          { name: 'CA Authority', status: 'online' },
        ]
      }
    } catch (e) {
      services.value = [
        { name: 'API Gateway', status: 'unknown' },
        { name: 'Identity Service', status: 'unknown' },
        { name: 'Cloud Sign', status: 'unknown' },
        { name: 'CA Authority', status: 'unknown' },
      ]
    }
    
  } catch (e) {
    console.error('Admin dashboard error:', e)
  } finally {
    loading.value = false
  }
}

const approve = async (id) => {
  if (!confirm('Xác nhận duyệt?')) return
  const authToken = token.value || localStorage.getItem('token')
  try {
    await fetch(`${apiBase.value}/admin/requests/${id}/approve`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    loadData()
  } catch (e) {
    alert('Lỗi khi duyệt')
  }
}

const reject = async (id) => {
  if (!confirm('Xác nhận từ chối?')) return
  const authToken = token.value || localStorage.getItem('token')
  try {
    await fetch(`${apiBase.value}/admin/requests/${id}/reject`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    loadData()
  } catch (e) {
    alert('Lỗi khi từ chối')
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
.stat-card { background: white; border: 1px solid #ddd; padding: 1.25rem; text-align: center; }
.stat-value { font-size: 1.5rem; font-weight: 700; color: #1a4d8c; }
.stat-label { font-size: 0.8rem; color: #666; }

.section { background: white; border: 1px solid #ddd; padding: 1.25rem; margin-bottom: 1rem; }
.section h3 { font-size: 0.95rem; margin-bottom: 1rem; padding-bottom: 0.5rem; border-bottom: 1px solid #eee; }

.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.text-center { text-align: center; color: #999; }

.btn { padding: 0.3rem 0.6rem; border: none; cursor: pointer; font-size: 0.75rem; margin-right: 0.25rem; }
.btn-primary { background: #1a4d8c; color: white; }
.btn-secondary { background: #6c757d; color: white; }

.service-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.75rem; }
.service-item { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0.75rem; background: #f8f9fa; font-size: 0.85rem; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.online { background: #28a745; }
.status-dot.offline { background: #dc3545; }
.status-dot.unknown { background: #ffc107; }
</style>
