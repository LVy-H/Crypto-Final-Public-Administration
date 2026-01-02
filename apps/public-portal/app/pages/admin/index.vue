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
  layout: 'admin',
  middleware: 'auth'
})

const { get, post } = useApi()
const loading = ref(true)
const stats = ref({ totalUsers: 0, activeCerts: 0, pendingRequests: 0, todaySignatures: 0 })
const pendingRequests = ref([])
const services = ref([])

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('vi-VN')
}

const loadData = async () => {
  try {
    loading.value = true
    
    // Load admin stats
    try {
      const statsData = await get('/admin/certificates/stats')
      if (statsData) stats.value = {
        totalUsers: 0, // Not available yet
        activeCerts: statsData.active || 0,
        pendingRequests: statsData.pending || 0,
        todaySignatures: 0
      }
    } catch (e) {
      console.warn('Admin stats not available', e)
    }
    
    // Load pending requests
    try {
      pendingRequests.value = await get('/admin/certificates/requests/pending')
    } catch (e) {
      console.warn('Pending requests not available', e)
    }
    
    // Load service health (Mock for now or use Actuator if available)
    services.value = [
      { name: 'API Gateway', status: 'online' },
      { name: 'Identity Service', status: 'online' },
      { name: 'Cloud Sign', status: 'online' },
      { name: 'CA Authority', status: 'online' },
    ]
    
  } catch (e) {
    console.error('Admin dashboard error:', e)
  } finally {
    loading.value = false
  }
}

const approve = async (id) => {
  if (!confirm('Xác nhận duyệt?')) return
  try {
    await post(`/admin/certificates/requests/${id}/approve`)
    await loadData() // Reload
  } catch (e) {
    alert('Lỗi khi duyệt: ' + (e.message || 'Unknown error'))
  }
}

const reject = async (id) => {
  const reason = prompt('Nhập lý do từ chối:', 'Admin rejected')
  if (reason === null) return // Cancelled

  try {
    await post(`/admin/certificates/requests/${id}/reject`, { reason })
    await loadData()
  } catch (e) {
    alert('Lỗi khi từ chối: ' + e.message)
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
