<template>
  <div class="dashboard">
      <h2 class="page-title">Tổng quan</h2>

      <div v-if="loading" class="loading">Đang tải...</div>

      <template v-else>
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalSigned }}</div>
            <div class="stat-label">Văn bản đã ký</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.verified }}</div>
            <div class="stat-label">Đã xác thực</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">Chờ xử lý</div>
          </div>
        </div>

        <div class="section">
          <h3>Chức năng</h3>
          <div class="action-grid">
            <NuxtLink to="/sign/upload" class="action-btn">
              <span class="icon">📝</span>
              <span>Ký văn bản</span>
            </NuxtLink>
            <NuxtLink to="/verify" class="action-btn">
              <span class="icon">🔍</span>
              <span>Xác thực</span>
            </NuxtLink>
            <NuxtLink to="/certificates" class="action-btn">
              <span class="icon">📜</span>
              <span>Chứng chỉ</span>
            </NuxtLink>
            <NuxtLink to="/history" class="action-btn">
              <span class="icon">📋</span>
              <span>Lịch sử</span>
            </NuxtLink>
          </div>
        </div>

        <div v-if="certInfo" class="section">
          <h3>Chứng chỉ</h3>
          <table class="info-table">
            <tr>
              <th>Thuật toán</th>
              <td>{{ certInfo.algorithm || 'ML-DSA-44' }}</td>
            </tr>
            <tr>
              <th>Trạng thái</th>
              <td><span class="badge badge-active">{{ certInfo.status || 'Hoạt động' }}</span></td>
            </tr>
            <tr>
              <th>Hết hạn</th>
              <td>{{ certInfo.expiresAt || 'N/A' }}</td>
            </tr>
          </table>
        </div>

        <div class="section">
          <h3>Hoạt động gần đây</h3>
          <table class="data-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Hoạt động</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="recentActivity.length === 0">
                <td colspan="3" class="text-center">Chưa có hoạt động</td>
              </tr>
              <tr v-for="item in recentActivity" :key="item.id">
                <td>{{ formatDate(item.createdAt) }}</td>
                <td>{{ item.action }}</td>
                <td><span :class="['badge', 'badge-' + item.status?.toLowerCase()]">{{ item.status }}</span></td>
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
const router = useRouter()
const { checkAuth, user, token } = useAuth()

const loading = ref(true)
const stats = ref({ totalSigned: 0, verified: 0, pending: 0 })
const certInfo = ref(null)
const recentActivity = ref([])

const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('vi-VN')
}

const loadDashboard = async () => {
  try {
    loading.value = true
    const authToken = token.value || localStorage.getItem('token')
    
    const headers = { 'Authorization': `Bearer ${authToken}` }
    
    // Load stats
    try {
      const statsRes = await fetch(`${apiBase.value}/user/stats`, { headers })
      if (statsRes.ok) stats.value = await statsRes.json()
    } catch (e) { console.warn('Stats not available') }
    
    // Load certificate info
    try {
      const certRes = await fetch(`${apiBase.value}/certificates/my`, { headers })
      if (certRes.ok) {
        const certs = await certRes.json()
        certInfo.value = certs[0] || null
      }
    } catch (e) { console.warn('Certs not available') }
    
    // Load recent activity
    try {
      const actRes = await fetch(`${apiBase.value}/user/activity?limit=5`, { headers })
      if (actRes.ok) recentActivity.value = await actRes.json()
    } catch (e) { console.warn('Activity not available') }
    
  } catch (e) {
    console.error('Dashboard load error:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!checkAuth()) {
    router.push('/login')
    return
  }
  loadDashboard()
})
</script>

<style scoped>
.dashboard { max-width: 900px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }

.loading { padding: 2rem; text-align: center; color: #666; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 1.5rem; }
.stat-card { background: white; border: 1px solid #ddd; padding: 1.25rem; text-align: center; }
.stat-value { font-size: 1.75rem; font-weight: 700; color: #1a4d8c; }
.stat-label { font-size: 0.8rem; color: #666; margin-top: 0.25rem; }

.section { background: white; border: 1px solid #ddd; padding: 1.25rem; margin-bottom: 1rem; }
.section h3 { font-size: 0.95rem; margin-bottom: 1rem; padding-bottom: 0.5rem; border-bottom: 1px solid #eee; }

.action-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.75rem; }
.action-btn { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; padding: 1rem; background: #f8f9fa; border: 1px solid #ddd; text-decoration: none; color: #333; font-size: 0.8rem; }
.action-btn:hover { background: #e8f0fe; border-color: #1a4d8c; }
.action-btn .icon { font-size: 1.5rem; }

.info-table { width: 100%; }
.info-table th, .info-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; }
.info-table th { width: 120px; color: #666; font-weight: 500; font-size: 0.85rem; }

.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.data-table th { background: #f8f9fa; font-weight: 600; color: #555; }

.badge { display: inline-block; padding: 0.2rem 0.5rem; font-size: 0.75rem; border-radius: 3px; }
.badge-active, .badge-success { background: #d4edda; color: #155724; }
.badge-pending { background: #fff3cd; color: #856404; }

.text-center { text-align: center; color: #999; }
</style>
