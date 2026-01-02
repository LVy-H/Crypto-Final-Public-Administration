<template>
  <div class="app-layout">
    <AppHeader />

    <div class="main-wrapper with-sidebar">
      <aside class="sidebar">
        <nav class="sidebar-nav">
          <NuxtLink to="/admin" class="sidebar-link">Tổng quan</NuxtLink>
          <NuxtLink to="/admin/users" class="sidebar-link">Người dùng</NuxtLink>
          <NuxtLink to="/admin/certificates" class="sidebar-link">Chứng chỉ</NuxtLink>
          <NuxtLink to="/admin/identity" class="sidebar-link">Xác thực KYC</NuxtLink>
          <NuxtLink to="/admin/audit" class="sidebar-link">Nhật ký</NuxtLink>
          <NuxtLink to="/admin/ca" class="sidebar-link">Quản lý CA</NuxtLink>
        </nav>
      </aside>

      <main class="main-content">
        <slot />
      </main>
    </div>

    <footer class="footer">
      <p>© 2025 Lê Việt Hoàng - Nguyễn Trọng Nhân</p>
    </footer>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.main-wrapper {
  flex: 1;
  display: flex;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
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
  overflow-x: auto;
}

.footer {
  background: #333;
  color: #999;
  text-align: center;
  padding: 1rem;
  font-size: 0.8rem;
}
</style>
