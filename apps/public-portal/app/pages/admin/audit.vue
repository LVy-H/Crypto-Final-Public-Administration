<template>

    <div class="admin-page">
      <h2 class="page-title">Nhật ký hệ thống</h2>

      <div class="section">
        <div class="toolbar">
          <select v-model="filter" class="filter-select">
            <option value="">Tất cả</option>
            <option value="AUTH">Xác thực</option>
            <option value="SIGN">Ký số</option>
            <option value="CERT">Chứng chỉ</option>
          </select>
        </div>
        
        <div v-if="loading" class="loading">Đang tải...</div>
        
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>Thời gian</th>
              <th>Người dùng</th>
              <th>Loại</th>
              <th>Hành động</th>
              <th>Kết quả</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredLogs.length === 0">
              <td colspan="5" class="text-center">Không có dữ liệu</td>
            </tr>
            <tr v-for="log in filteredLogs" :key="log.id">
              <td>{{ formatDate(log.timestamp) }}</td>
              <td>{{ log.username }}</td>
              <td>{{ log.type }}</td>
              <td>{{ log.action }}</td>
              <td><span :class="['badge', 'badge-' + log.result?.toLowerCase()]">{{ log.result }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

</template>

<script setup>
definePageMeta({
  middleware: 'auth'
})

const config = useRuntimeConfig()
const { token } = useAuth()

const filter = ref('')
const logs = ref([])
const loading = ref(true)

const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('vi-VN')
}

const filteredLogs = computed(() => {
  if (!filter.value) return logs.value
  return logs.value.filter(l => l.type === filter.value)
})

const loadData = async () => {
  try {
    loading.value = true
    const authToken = token.value || localStorage.getItem('token')
    
    const res = await fetch(`${apiBase.value}/admin/audit?limit=100`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    
    if (res.ok) {
      logs.value = await res.json()
    }
  } catch (e) {
    console.error('Error loading audit logs:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-page { max-width: 900px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }
.section { background: white; border: 1px solid #ddd; padding: 1.25rem; }
.toolbar { margin-bottom: 1rem; }
.filter-select { padding: 0.5rem 0.75rem; border: 1px solid #ddd; font-size: 0.85rem; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.badge { padding: 0.2rem 0.5rem; font-size: 0.7rem; border-radius: 3px; }
.badge-success { background: #d4edda; color: #155724; }
.badge-pending { background: #ffc107; color: #333; }
.badge-error, .badge-failure { background: #f8d7da; color: #721c24; }
.text-center { text-align: center; color: #999; }
.loading { padding: 1rem; text-align: center; color: #666; }
</style>
