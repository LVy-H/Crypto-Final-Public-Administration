<template>
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
</template>

<script setup>
const router = useRouter()
const { user, isAuthenticated, hasAdminAccess, logout } = useAuth()

const handleLogout = () => {
  logout()
  router.push('/login')
}
</script>

<style scoped>
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
</style>
