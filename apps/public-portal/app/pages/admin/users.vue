<template>

    <div class="admin-page">
      <h2 class="page-title">Quản lý người dùng</h2>

      <div class="section">
        <div class="toolbar">
          <input v-model="search" type="text" placeholder="Tìm kiếm..." class="search-input" />
        </div>
        
        <div v-if="loading" class="loading">Đang tải...</div>
        
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Email</th>
              <th>Vai trò</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredUsers.length === 0">
              <td colspan="5" class="text-center">Không có dữ liệu</td>
            </tr>
            <tr v-for="user in filteredUsers" :key="user.username">
              <td>{{ user.username }}</td>
              <td>{{ user.email }}</td>
              <td>{{ user.role }}</td>
              <td><span :class="['badge', 'badge-' + (user.enabled ? 'active' : 'inactive')]">{{ user.enabled ? 'Hoạt động' : 'Vô hiệu' }}</span></td>
              <td>
                <button class="btn btn-sm">Chi tiết</button>
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

const search = ref('')
const users = ref([])
const loading = ref(true)

const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

const filteredUsers = computed(() => {
  if (!search.value) return users.value
  const q = search.value.toLowerCase()
  return users.value.filter(u => 
    u.username?.toLowerCase().includes(q) || 
    u.email?.toLowerCase().includes(q)
  )
})

const loadData = async () => {
  try {
    loading.value = true
    const authToken = token.value || localStorage.getItem('token')
    
    const res = await fetch(`${apiBase.value}/admin/users`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    
    if (res.ok) {
      users.value = await res.json()
    }
  } catch (e) {
    console.error('Error loading users:', e)
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
.search-input { padding: 0.5rem 0.75rem; border: 1px solid #ddd; width: 250px; font-size: 0.85rem; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.btn { padding: 0.3rem 0.6rem; border: 1px solid #ddd; background: white; cursor: pointer; font-size: 0.75rem; }
.badge { padding: 0.2rem 0.5rem; font-size: 0.7rem; border-radius: 3px; }
.badge-active { background: #d4edda; color: #155724; }
.badge-inactive { background: #f8d7da; color: #721c24; }
.text-center { text-align: center; color: #999; }
.loading { padding: 1rem; text-align: center; color: #666; }
</style>
