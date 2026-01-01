<template>

    <div class="admin-page">
      <h2 class="page-title">Xác thực danh tính</h2>

      <div class="section">
        <h3>Yêu cầu chờ duyệt</h3>
        
        <div v-if="loading" class="loading">Đang tải...</div>
        <div v-else-if="error" class="error">{{ error }}</div>
        
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Email</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pendingUsers.length === 0">
              <td colspan="4" class="text-center">Không có yêu cầu</td>
            </tr>
            <tr v-for="user in pendingUsers" :key="user.username">
              <td>{{ user.username }}</td>
              <td>{{ user.email }}</td>
              <td><span class="badge badge-pending">{{ user.identityStatus }}</span></td>
              <td>
                <button @click="approveUser(user.username)" class="btn btn-primary btn-sm">Duyệt</button>
                <button @click="rejectUser(user.username)" class="btn btn-secondary btn-sm">Từ chối</button>
              </td>
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

const pendingUsers = ref([])
const loading = ref(true)
const error = ref(null)

const apiBase = computed(() => config.public.apiBase || '/api/v1')

const loadData = async () => {
  try {
    loading.value = true
    error.value = null
    const authToken = token.value || localStorage.getItem('token')
    
    const res = await fetch(`${apiBase.value}/identity/pending`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    
    if (!res.ok) {
      throw new Error('Không thể tải dữ liệu')
    }
    
    pendingUsers.value = await res.json()
  } catch (e) {
    error.value = e.message
    pendingUsers.value = []
  } finally {
    loading.value = false
  }
}

const approveUser = async (username) => {
  if (!confirm(`Duyệt người dùng ${username}?`)) return
  
  try {
    const authToken = token.value || localStorage.getItem('token')
    const res = await fetch(`${apiBase.value}/identity/approve/${username}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    
    if (res.ok) {
      alert('Đã duyệt')
      loadData()
    } else {
      alert('Lỗi khi duyệt')
    }
  } catch (e) {
    alert('Lỗi kết nối')
  }
}

const rejectUser = async (username) => {
  if (!confirm(`Từ chối người dùng ${username}?`)) return
  
  try {
    const authToken = token.value || localStorage.getItem('token')
    const res = await fetch(`${apiBase.value}/identity/reject/${username}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    
    if (res.ok) {
      alert('Đã từ chối')
      loadData()
    } else {
      alert('Lỗi khi từ chối')
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
.admin-page { max-width: 900px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }
.section { background: white; border: 1px solid #ddd; padding: 1.25rem; }
.section h3 { font-size: 0.95rem; margin-bottom: 1rem; padding-bottom: 0.5rem; border-bottom: 1px solid #eee; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.btn { padding: 0.3rem 0.6rem; border: none; cursor: pointer; font-size: 0.75rem; margin-right: 0.25rem; }
.btn-primary { background: #1a4d8c; color: white; }
.btn-secondary { background: #6c757d; color: white; }
.badge { padding: 0.2rem 0.5rem; font-size: 0.7rem; border-radius: 3px; }
.badge-pending { background: #ffc107; color: #333; }
.text-center { text-align: center; color: #999; }
.loading, .error { padding: 1rem; text-align: center; }
.error { color: red; }
</style>
