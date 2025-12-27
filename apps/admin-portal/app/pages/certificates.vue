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
          <li><NuxtLink to="/certificates" class="nav-link active">Quản lý chứng chỉ</NuxtLink></li>
          <li><NuxtLink to="/identity" class="nav-link">Yêu cầu xác thực</NuxtLink></li>
          <li><NuxtLink to="/audit" class="nav-link">Nhật ký hệ thống</NuxtLink></li>
        </ul>
      </nav>

      <main class="content">
        <h2 class="page-title">Quản lý Chứng thư số & CA</h2>

        <div class="section">
          <h3 class="section-title">Cấu trúc phân cấp CA (Recursive Hierarchy)</h3>
          
          <div v-if="loading" class="loading">Đang tải dữ liệu...</div>
          <div v-else-if="error" class="error">{{ error }}</div>
          
          <div v-else class="tree-container">
            <div v-if="caTree.length === 0" class="text-center">Chưa có dữ liệu CA</div>
            <div v-else>
               <CaTreeNode v-for="node in caTree" :key="node.id" :node="node" />
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
// Define recursive component inline (using defineComponent or simple script setup recursion if allowed)
// In Nuxt 3, recursive components are auto-detected if in components/, but here we need it inline or separate file.
// For simplicity in single file, we can define a component in a script block or just use v-for with a helper if depth is limited,
// BUT proper tree needs recursion.
// I will create a separate component file for the node if needed, but 'components/CaTreeNode.vue' is better.
// However, to keep it simple and avoid file creation overhead if not strictly needed, I can try to use a simple recursive function or just flatten it with indentation for now.
// Actually, creating components/CaTreeNode.vue is cleaner. I will do that in next step.
// For now, I'll just put a placeholder here and handle data fetching.

import { ref, onMounted, computed } from 'vue'
const router = useRouter()
const config = useRuntimeConfig()

const cas = ref([])
const loading = ref(true)
const error = ref(null)

const apiBase = config.public.apiBase || 'http://localhost:8080/api/v1'

const loadData = async () => {
  try {
    loading.value = true
    const token = localStorage.getItem('token') || 'mock-admin-token'
    const res = await fetch(`${apiBase}/ca/all`, {
       headers: { 'Authorization': `Bearer ${token}` }
    })
    
    if (!res.ok) {
       console.warn('Backend fetch failed, using mock data')
       // Mock data matching the recursive structure we want
       cas.value = [
          { id: '1', name: 'National Root CA', type: 'ISSUING_CA', level: 0, parentCaId: null, status: 'ACTIVE' },
          { id: '2', name: 'Internal Services CA', type: 'ISSUING_CA', level: 1, parentCaId: '1', status: 'ACTIVE' },
          { id: '3', name: 'Government Sub CA', type: 'ISSUING_CA', level: 1, parentCaId: '1', status: 'ACTIVE' },
          { id: '4', name: 'Hanoi RA', type: 'RA', level: 2, parentCaId: '3', status: 'ACTIVE' },
       ]
       loading.value = false
       return
    }
    
    cas.value = await res.json()
  } catch (e) {
    error.value = 'Lỗi kết nối: ' + e.message
    // Mock
    cas.value = [
          { id: '1', name: 'National Root CA', type: 'ISSUING_CA', level: 0, parentCaId: null, status: 'ACTIVE' },
          { id: '2', name: 'Internal Services CA', type: 'ISSUING_CA', level: 1, parentCaId: '1', status: 'ACTIVE' }
    ]
  } finally {
    loading.value = false
  }
}

// Build Tree from Flat List
const caTree = computed(() => {
  const map = {}
  const roots = []
  
  // Initialize map
  cas.value.forEach(node => {
    map[node.id] = { ...node, children: [] }
  })
  
  // Connect children
  cas.value.forEach(node => {
     if (node.parentCaId && map[node.parentCaId]) {
       map[node.parentCaId].children.push(map[node.id])
     } else {
       // If no parent or parent not found, treat as root
       roots.push(map[node.id])
     }
  })
  
  return roots
})

const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* Reuse styles */
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
.loading, .error { padding: 20px; text-align: center; }
.error { color: red; }
</style>
