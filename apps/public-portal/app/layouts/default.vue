<template>
  <div class="app-layout">
    <!-- Header -->
    <header class="header">
      <div class="header-content">
        <div class="header-left">
          <NuxtLink to="/" class="brand">
            <span class="brand-icon">🏛️</span>
            <span class="brand-text">Hệ thống Chữ ký số</span>
          </NuxtLink>
        </div>
        
        <nav v-if="isAuthenticated" class="header-nav">
          <NuxtLink to="/dashboard" class="nav-link">Tổng quan</NuxtLink>
          <NuxtLink to="/certificates" class="nav-link">Chứng chỉ</NuxtLink>
          <NuxtLink to="/sign/upload" class="nav-link">Ký văn bản</NuxtLink>
          <NuxtLink to="/verify" class="nav-link">Xác thực</NuxtLink>
          <NuxtLink v-if="hasAdminAccess" to="/admin" class="nav-link nav-admin">Quản trị</NuxtLink>
        </nav>

        <div class="header-right">
          <template v-if="isAuthenticated">
            <span class="user-name">{{ user?.username }}</span>
            <span v-if="hasAdminAccess" class="role-badge">{{ user?.role }}</span>
            <button @click="handleLogout" class="btn-logout">Đăng xuất</button>
          </template>
          <template v-else>
            <NuxtLink to="/login" class="btn-link">Đăng nhập</NuxtLink>
            <NuxtLink to="/register" class="btn-primary">Đăng ký</NuxtLink>
          </template>
        </div>
      </div>
    </header>

    <!-- Sidebar for admin routes -->
    <div class="main-wrapper" :class="{ 'with-sidebar': showAdminSidebar }">
      <aside v-if="showAdminSidebar" class="sidebar">
        <nav class="sidebar-nav">
          <NuxtLink to="/admin" class="sidebar-link">Tổng quan</NuxtLink>
          <NuxtLink to="/admin/users" class="sidebar-link">Người dùng</NuxtLink>
          <NuxtLink to="/admin/certificates" class="sidebar-link">Chứng chỉ</NuxtLink>
          <NuxtLink to="/admin/identity" class="sidebar-link">Xác thực KYC</NuxtLink>
          <NuxtLink to="/admin/audit" class="sidebar-link">Nhật ký</NuxtLink>
        </nav>
      </aside>

      <main class="main-content">
        <slot />
      </main>
    </div>

    <!-- Footer -->
    <footer class="footer">
      <p>© 2025 Lê Việt Hoàng - Nguyễn Trọng Nhân</p>
    </footer>
  </div>
</template>

<script setup>
const route = useRoute()
const router = useRouter()
const { user, isAuthenticated, hasAdminAccess, logout, checkAuth } = useAuth()

const showAdminSidebar = computed(() => 
  route.path.startsWith('/admin') && hasAdminAccess.value
)

const handleLogout = () => {
  logout()
  router.push('/login')
}

onMounted(() => {
  checkAuth()
})
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.header {
  background: #1a4d8c;
  color: white;
  border-bottom: 3px solid #c41e3a;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0.75rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

.header-left {
  flex-shrink: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: white;
  text-decoration: none;
  font-weight: 600;
}

.brand-icon { font-size: 1.5rem; }
.brand-text { font-size: 1rem; }

.header-nav {
  display: flex;
  gap: 0.25rem;
  flex: 1;
}

.nav-link {
  padding: 0.5rem 0.75rem;
  color: rgba(255,255,255,0.85);
  text-decoration: none;
  font-size: 0.875rem;
  border-radius: 4px;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: rgba(255,255,255,0.15);
  color: white;
}

.nav-admin {
  margin-left: auto;
  background: rgba(196, 30, 58, 0.3);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.user-name {
  font-size: 0.875rem;
}

.role-badge {
  font-size: 0.7rem;
  padding: 0.15rem 0.4rem;
  background: rgba(255,255,255,0.2);
  border-radius: 3px;
}

.btn-logout {
  background: transparent;
  border: 1px solid rgba(255,255,255,0.4);
  color: white;
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
  cursor: pointer;
  border-radius: 3px;
}

.btn-logout:hover {
  background: rgba(255,255,255,0.1);
}

.btn-link {
  color: white;
  text-decoration: none;
  font-size: 0.875rem;
}

.btn-primary {
  background: white;
  color: #1a4d8c;
  padding: 0.4rem 0.85rem;
  font-size: 0.8rem;
  text-decoration: none;
  border-radius: 3px;
  font-weight: 500;
}

.main-wrapper {
  flex: 1;
  display: flex;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
}

.main-wrapper.with-sidebar {
  padding: 0;
}

.sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #ddd;
  flex-shrink: 0;
}

.sidebar-nav {
  padding: 1rem 0;
}

.sidebar-link {
  display: block;
  padding: 0.65rem 1.25rem;
  color: #333;
  text-decoration: none;
  font-size: 0.875rem;
  border-left: 3px solid transparent;
}

.sidebar-link:hover,
.sidebar-link.router-link-active {
  background: #e8f0fe;
  border-left-color: #1a4d8c;
  color: #1a4d8c;
}

.main-content {
  flex: 1;
  padding: 1.5rem;
}

.footer {
  background: #333;
  color: #999;
  text-align: center;
  padding: 1rem;
  font-size: 0.8rem;
}
</style>
