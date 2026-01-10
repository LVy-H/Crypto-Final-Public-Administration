<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface Request {
  id: string
  type: 'RA_REQUEST' | 'SIGNING_REQUEST'
  requester: string
  submittedAt: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
}

const requests = ref<Request[]>([])
const loading = ref(true)
const stats = ref({ totalUsers: 152, activeCerts: 43, pendingRequests: 2, todaySignatures: 18 })
const services = ref([
  { name: 'API Gateway', status: 'online' },
  { name: 'Identity Service', status: 'online' },
  { name: 'Cloud Sign', status: 'online' },
  { name: 'CA Authority', status: 'online' }
])

function loadRequests() {
  loading.value = true
  setTimeout(() => {
    requests.value = [
      {
        id: 'REQ-RA-2025-001',
        type: 'RA_REQUEST',
        requester: 'nguyenvana',
        submittedAt: '2025-10-25T10:30:00Z',
        status: 'PENDING'
      },
      {
        id: 'REQ-SIGN-2025-089',
        type: 'SIGNING_REQUEST',
        requester: 'lethib (Org)',
        submittedAt: '2025-10-25T11:15:00Z',
        status: 'PENDING'
      }
    ]
    loading.value = false
  }, 600)
}

function reviewRequest(id: string) {
  router.push(`/officer/review/${id}`)
}

onMounted(() => {
  loadRequests()
})
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h2>Quản trị hệ thống (Officer Portal)</h2>
    </div>

    <div v-if="loading" class="loading">Đang tải dữ liệu...</div>

    <template v-else>
      <!-- Stats Area -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="val">{{ stats.totalUsers }}</div>
          <div class="lbl">Người dùng</div>
        </div>
        <div class="stat-card">
          <div class="val">{{ stats.activeCerts }}</div>
          <div class="lbl">Hồ sơ CKS</div>
        </div>
        <div class="stat-card highlight">
          <div class="val">{{ stats.pendingRequests }}</div>
          <div class="lbl">Chờ duyệt</div>
        </div>
        <div class="stat-card">
          <div class="val">{{ stats.todaySignatures }}</div>
          <div class="lbl">Lượt ký hôm nay</div>
        </div>
      </div>

      <!-- Pending Requests -->
      <div class="section">
        <h3>Yêu cầu chờ xử lý</h3>
        <table class="req-table">
          <thead>
            <tr>
              <th>Mã YC</th>
              <th>Người gửi</th>
              <th>Loại yêu cầu</th>
              <th>Thời gian</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in requests" :key="r.id">
              <td class="mono">{{ r.id }}</td>
              <td>{{ r.requester }}</td>
              <td>
                <span :class="['type-tag', r.type === 'RA_REQUEST' ? 'ra' : 'sign']">
                  {{ r.type === 'RA_REQUEST' ? 'Cấp CKS' : 'Ký duyệt' }}
                </span>
              </td>
              <td>{{ new Date(r.submittedAt).toLocaleString('vi-VN') }}</td>
              <td>
                <button @click="reviewRequest(r.id)" class="btn-review">Xem xét</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Service Health -->
      <div class="section">
        <h3>Trạng thái dịch vụ</h3>
        <div class="svc-grid">
          <div v-for="svc in services" :key="svc.name" class="svc-item">
            <span :class="['dot', svc.status]"></span>
            <span>{{ svc.name }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-container { max-width: 1000px; margin: 2rem auto; padding: 1rem; }
h2 { color: #1a4d8c; margin-bottom: 1.5rem; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 2rem; }
.stat-card { background: white; padding: 1.5rem; text-align: center; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); border: 1px solid #eee; }
.stat-card.highlight { border-color: #f57c00; background: #fff8e1; }
.val { font-size: 2rem; font-weight: 700; color: #1a4d8c; }
.lbl { color: #666; font-size: 0.9rem; margin-top: 0.5rem; }

.section { background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); margin-bottom: 2rem; border: 1px solid #eee; }
.section h3 { font-size: 1.1rem; margin-bottom: 1rem; border-bottom: 1px solid #eee; padding-bottom: 0.5rem; }

.req-table { width: 100%; border-collapse: collapse; }
.req-table th, .req-table td { padding: 1rem; text-align: left; border-bottom: 1px solid #eee; }
.req-table th { background: #f8f9fa; font-weight: 600; color: #444; }
.mono { font-family: monospace; color: #555; }
.btn-review { background: #1a4d8c; color: white; border: none; padding: 0.5rem 1rem; border-radius: 4px; cursor: pointer; font-weight: 500; }
.btn-review:hover { background: #153d6e; }

.svc-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }
.svc-item { display: flex; align-items: center; gap: 0.5rem; background: #f8f9fa; padding: 0.8rem; border-radius: 4px; font-weight: 500; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot.online { background: #28a745; }
</style>
