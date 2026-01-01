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
            <NuxtLink to="/settings/security" class="action-btn">
              <span class="icon">🔐</span>
              <span>Bảo mật</span>
            </NuxtLink>
          </div>
        </div>

        <div v-if="certInfo" class="section">
          <h3>Chứng chỉ</h3>
          <table class="info-table">
            <tbody>
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
            </tbody>
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

const apiBase = computed(() => config.public.apiBase || '/api/v1')

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('vi-VN')
}

const { get } = useApi()

const loadDashboard = async () => {
  try {
    loading.value = true
    
    // Load stats
    try {
      stats.value = await get('/user/stats')
    } catch (e) { console.warn('Stats not available') }
    
    // Load certificate info
    try {
      const certs = await get('/certificates/my')
      certInfo.value = certs[0] || null
    } catch (e) { console.warn('Certs not available') }
    
    // Load recent activity
    try {
      recentActivity.value = await get('/user/activity?limit=5')
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
.dashboard { max-width: 1000px; margin: 0 auto; }
.page-title { font-size: 1.5rem; color: #1a4d8c; margin-bottom: 2rem; font-weight: 600; }

.loading { padding: 3rem; text-align: center; color: #666; font-size: 1.1rem; }

/* Stats Row - Making it cleaner */
.stats-row { 
  display: grid; 
  grid-template-columns: repeat(3, 1fr); 
  gap: 1.5rem; 
  margin-bottom: 2.5rem; 
}
.stat-card { 
  background: white; 
  border: 1px solid #e1e4e8; 
  border-radius: 8px;
  padding: 1.5rem; 
  text-align: center; 
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border-color: #1a4d8c;
}
.stat-value { font-size: 2.5rem; font-weight: 700; color: #1a4d8c; line-height: 1.2; }
.stat-label { font-size: 0.9rem; color: #666; margin-top: 0.5rem; font-weight: 500; }

/* Sections */
.section { 
  background: white; 
  border: 1px solid #e1e4e8; 
  border-radius: 8px;
  padding: 1.5rem; 
  margin-bottom: 2rem; 
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.section h3 { 
  font-size: 1.1rem; 
  margin-bottom: 1.25rem; 
  padding-bottom: 0.75rem; 
  border-bottom: 1px solid #eee; 
  color: #2c3e50;
  font-weight: 600;
}

/* Action Grid - Making buttons look more clickable/premium */
.action-grid { 
  display: grid; 
  grid-template-columns: repeat(4, 1fr); 
  gap: 1rem; 
}
.action-btn { 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  gap: 0.75rem; 
  padding: 1.5rem; 
  background: #f8f9fa; 
  border: 1px solid #eee; 
  border-radius: 8px;
  text-decoration: none; 
  color: #495057; 
  font-size: 0.95rem; 
  font-weight: 500;
  transition: all 0.2s;
}
.action-btn:hover { 
  background: white; 
  border-color: #1a4d8c; 
  color: #1a4d8c; 
  box-shadow: 0 4px 12px rgba(26, 77, 140, 0.1);
}
.action-btn .icon { font-size: 2rem; }

/* Tables - Cleaner look */
.info-table, .data-table { width: 100%; border-collapse: separate; border-spacing: 0; }
.info-table th, .info-table td, .data-table th, .data-table td { 
  padding: 1rem; 
  text-align: left; 
  border-bottom: 1px solid #eee; 
}
.info-table th { width: 140px; color: #666; font-weight: 500; white-space: nowrap; }
.info-table td { color: #333; font-weight: 500; }

.data-table th { 
  background: #f8f9fa; 
  font-weight: 600; 
  color: #495057; 
  border-bottom: 2px solid #e9ecef;
}
.data-table tr:last-child td { border-bottom: none; }

/* Badges - Modern style */
.badge { 
  display: inline-flex; 
  align-items: center;
  padding: 0.25rem 0.6rem; 
  font-size: 0.75rem; 
  border-radius: 20px; 
  font-weight: 600;
  letter-spacing: 0.3px;
}
.badge-active, .badge-success, .badge-completed { background: #d4edda; color: #155724; }
.badge-pending { background: #fff3cd; color: #856404; }
.badge-revoked, .badge-failed { background: #f8d7da; color: #721c24; }

.text-center { text-align: center; color: #999; padding: 2rem; }
</style>
