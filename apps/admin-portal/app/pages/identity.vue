<template>
  <div class="admin-dashboard">
    <header class="header">
      <div class="header-content">
        <div class="header-left">
          <span class="logo">🏛️</span>
          <h1 class="site-title">Hệ thống Chữ ký số - Quản trị</h1>
        </div>
        <div class="header-right">
          <span class="user-info">Quản trị viên</span>
          <button @click="logout" class="btn-link">Đăng xuất</button>
        </div>
      </div>
    </header>

    <div class="main-container">
      <nav class="sidebar">
        <ul class="nav-list">
          <li><NuxtLink to="/dashboard" class="nav-link">Tổng quan</NuxtLink></li>
          <li><NuxtLink to="/users" class="nav-link">Quản lý người dùng</NuxtLink></li>
          <li><NuxtLink to="/certificates" class="nav-link">Quản lý chứng chỉ</NuxtLink></li>
          <li><NuxtLink to="/identity" class="nav-link active">Yêu cầu xác thực</NuxtLink></li>
          <li><NuxtLink to="/audit" class="nav-link">Nhật ký hệ thống</NuxtLink></li>
        </ul>
      </nav>

      <main class="content">
        <h2 class="page-title">Quản lý xác thực danh tính</h2>

        <div class="section">
          <h3 class="section-title">Yêu cầu chờ duyệt</h3>
          
          <div v-if="loading" class="loading">Đang tải dữ liệu...</div>
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
                <td colspan="4" class="text-center">Không có yêu cầu nào</td>
              </tr>
              <tr v-for="user in pendingUsers" :key="user.username">
                <td>{{ user.username }}</td>
                <td>{{ user.email }}</td>
                <td><span class="badge badge-warning">{{ user.identityStatus }}</span></td>
                <td>
                  <button @click="approveUser(user.username)" class="btn btn-sm btn-primary">Duyệt</button>
                  <button class="btn btn-sm btn-secondary">Từ chối</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
const router = useRouter()
const config = useRuntimeConfig()

const pendingUsers = ref([])
const loading = ref(true)
const error = ref(null)

// For demo/dev, use localhost:8080 if not configured
const apiBase = config.public.apiBase || 'http://localhost:8080/api/v1'

const loadData = async () => {
  try {
    loading.value = true
    // Simulate token (in real app, get from storage)
    const token = localStorage.getItem('token') || 'mock-admin-token'
    
    const res = await fetch(`${apiBase}/identity/pending`, {
      headers: {
        'Authorization': `Bearer ${token}` 
      }
    })
    
    if (!res.ok) {
       // Fallback for demo if backend not running or auth fails during dev
       console.warn('Backend fetch failed, using mock data')
       pendingUsers.value = [
          { username: 'nguyenvana', email: 'a@test.com', identityStatus: 'PENDING' },
          { username: 'tranthib', email: 'b@test.com', identityStatus: 'PENDING' }
       ]
       loading.value = false
       return
    }
    
    pendingUsers.value = await res.json()
  } catch (e) {
    error.value = 'Lỗi kết nối: ' + e.message
    // Mock fallback
      pendingUsers.value = [
          { username: 'nguyenvana', email: 'a@test.com', identityStatus: 'PENDING' },
          { username: 'tranthib', email: 'b@test.com', identityStatus: 'PENDING' }
       ]
  } finally {
    loading.value = false
  }
}

const approveUser = async (username) => {
  if (!confirm(`Xác nhận duyệt người dùng ${username}?`)) return
  
  try {
     const token = localStorage.getItem('token') || 'mock-admin-token'
     const res = await fetch(`${apiBase}/identity/approve/${username}`, {
       method: 'POST',
       headers: {
         'Authorization': `Bearer ${token}`
       }
     })
     
     if (res.ok) {
       alert('Đã duyệt thành công!')
       loadData() // Reload
     } else {
       alert('Lỗi khi duyệt!')
     }
  } catch (e) {
    alert('Lỗi kết nối!')
  }
}

const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* Reuse styles from dashboard */
.admin-dashboard { min-height: 100vh; }
.header { background: #1a4d8c; color: white; padding: 12px 24px; border-bottom: 3px solid #c41e3a; }
.header-content { display: flex; justify-content: space-between; align-items: center; max-width: 1400px; margin: 0 auto; }
.header-left { display: flex; align-items: center; gap: 12px; }
.logo { font-size: 24px; }
.site-title { font-size: 18px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 16px; }
.user-info { font-size: 14px; }
.btn-link { background: none; border: none; color: white; cursor: pointer; text-decoration: underline; }
.main-container { display: flex; max-width: 1400px; margin: 0 auto; }
.sidebar { width: 220px; background: white; border-right: 1px solid #ddd; min-height: calc(100vh - 60px); padding: 16px 0; }
.nav-list { list-style: none; }
.nav-link { display: block; padding: 12px 20px; color: #333; text-decoration: none; border-left: 3px solid transparent; }
.nav-link:hover, .nav-link.active { background: #e8f0fe; border-left-color: #1a4d8c; color: #1a4d8c; }
.content { flex: 1; padding: 24px; }
.page-title { font-size: 20px; font-weight: 600; margin-bottom: 24px; color: #1a4d8c; }
.section { background: white; border: 1px solid #ddd; padding: 20px; margin-bottom: 24px; }
.section-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid #eee; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #eee; }
.data-table th { background: #f8f9fa; font-weight: 600; font-size: 13px; color: #555; }
.btn { padding: 6px 12px; border: none; cursor: pointer; font-size: 12px; margin-right: 4px; }
.btn-primary { background: #1a4d8c; color: white; }
.btn-secondary { background: #6c757d; color: white; }
.btn-sm { padding: 4px 8px; }
.badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; }
.badge-warning { background: #ffc107; color: #333; }
.text-center { text-align: center; }
.loading, .error { padding: 20px; text-align: center; }
.error { color: red; }
</style>
