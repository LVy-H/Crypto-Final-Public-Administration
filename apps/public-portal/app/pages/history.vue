<template>

    <div class="page-content">
      <h2 class="page-title">Lịch sử hoạt động</h2>

      <div class="filter-bar">
        <select v-model="filter" class="select">
          <option value="all">Tất cả</option>
          <option value="SIGN">Ký văn bản</option>
          <option value="VERIFY">Xác thực</option>
        </select>
      </div>

      <div v-if="loading" class="loading">Đang tải...</div>

      <div v-else class="section">
        <table class="data-table">
          <thead>
            <tr>
              <th>Thời gian</th>
              <th>Hoạt động</th>
              <th>Văn bản</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredActivities.length === 0">
              <td colspan="4" class="text-center">Không có hoạt động</td>
            </tr>
            <tr v-for="item in filteredActivities" :key="item.id">
              <td>{{ formatDate(item.createdAt) }}</td>
              <td>{{ item.type === 'SIGN' ? 'Ký văn bản' : 'Xác thực' }}</td>
              <td>{{ item.documentName || item.filename }}</td>
              <td><span class="status-badge" :class="item.status?.toLowerCase()">{{ item.statusText }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

</template>

<script setup>
definePageMeta({ middleware: 'auth' })

const config = useRuntimeConfig()
const { token } = useAuth()

const loading = ref(true)
const filter = ref('all')
const activities = ref([])

const apiBase = computed(() => config.public.apiBase || '/api/v1')

const filteredActivities = computed(() => {
  if (filter.value === 'all') return activities.value
  return activities.value.filter(a => a.type === filter.value)
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('vi-VN')
}

onMounted(async () => {
  try {
    const authToken = token.value || localStorage.getItem('token')
    const res = await fetch(`${apiBase.value}/user/activity?limit=50`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    if (res.ok) {
      const data = await res.json()
      activities.value = data.map(a => ({
        ...a,
        statusText: a.status === 'SUCCESS' ? 'Hoàn thành' : (a.status === 'PENDING' ? 'Đang chờ' : 'Lỗi')
      }))
    }
  } catch (e) {
    console.error('Error loading history:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-content { max-width: 900px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }
.loading { padding: 2rem; text-align: center; color: #666; }

.filter-bar { background: white; border: 1px solid #ddd; padding: 0.75rem; margin-bottom: 1rem; }
.select { padding: 0.5rem; border: 1px solid #ddd; font-size: 0.85rem; }

.section { background: white; border: 1px solid #ddd; }

.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.data-table th { background: #f8f9fa; font-weight: 600; color: #555; }
.text-center { text-align: center; color: #999; }

.status-badge { padding: 0.2rem 0.5rem; font-size: 0.75rem; border-radius: 3px; }
.status-badge.success { background: #d4edda; color: #155724; }
.status-badge.pending { background: #fff3cd; color: #856404; }
.status-badge.error { background: #f8d7da; color: #721c24; }
</style>
