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
          <li><NuxtLink to="/dashboard" class="nav-link active">Tổng quan</NuxtLink></li>
          <li><NuxtLink to="/users" class="nav-link">Quản lý người dùng</NuxtLink></li>
          <li><NuxtLink to="/certificates" class="nav-link">Quản lý chứng chỉ</NuxtLink></li>
          <li><NuxtLink to="/requests" class="nav-link">Yêu cầu chờ duyệt</NuxtLink></li>
          <li><NuxtLink to="/audit" class="nav-link">Nhật ký hệ thống</NuxtLink></li>
        </ul>
      </nav>

      <main class="content">
        <h2 class="page-title">Tổng quan hệ thống</h2>
        
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">Tổng người dùng</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.activeCerts }}</div>
            <div class="stat-label">Chứng chỉ hoạt động</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.pendingRequests }}</div>
            <div class="stat-label">Yêu cầu chờ duyệt</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.todaySignatures }}</div>
            <div class="stat-label">Ký số hôm nay</div>
          </div>
        </div>

        <div class="section">
          <h3 class="section-title">Yêu cầu chờ xử lý</h3>
          <table class="data-table">
            <thead>
              <tr>
                <th>Mã yêu cầu</th>
                <th>Người dùng</th>
                <th>Loại yêu cầu</th>
                <th>Ngày gửi</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="req in pendingRequests" :key="req.id">
                <td>{{ req.id }}</td>
                <td>{{ req.user }}</td>
                <td>{{ req.type }}</td>
                <td>{{ req.date }}</td>
                <td>
                  <button class="btn btn-sm btn-primary">Duyệt</button>
                  <button class="btn btn-sm btn-secondary">Từ chối</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="section">
          <h3 class="section-title">Trạng thái dịch vụ</h3>
          <div class="service-status">
            <div v-for="svc in services" :key="svc.name" class="service-item">
              <span class="status-indicator" :class="svc.status"></span>
              <span class="service-name">{{ svc.name }}</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
const router = useRouter()

const stats = ref({
  totalUsers: 1247,
  activeCerts: 892,
  pendingRequests: 23,
  todaySignatures: 156
})

const pendingRequests = ref([
  { id: 'REQ-001', user: 'Nguyễn Văn A', type: 'Cấp chứng chỉ mới', date: '25/12/2024' },
  { id: 'REQ-002', user: 'Trần Thị B', type: 'Gia hạn chứng chỉ', date: '25/12/2024' },
  { id: 'REQ-003', user: 'Lê Văn C', type: 'Xác thực KYC', date: '24/12/2024' },
])

const services = ref([
  { name: 'API Gateway', status: 'online' },
  { name: 'Identity Service', status: 'online' },
  { name: 'Cloud Sign', status: 'online' },
  { name: 'CA Authority', status: 'online' },
  { name: 'Validation Service', status: 'online' },
])

const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: 'Segoe UI', Arial, sans-serif;
  background: #f5f5f5;
  color: #333;
  font-size: 14px;
}

.admin-dashboard {
  min-height: 100vh;
}

.header {
  background: #1a4d8c;
  color: white;
  padding: 12px 24px;
  border-bottom: 3px solid #c41e3a;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo { font-size: 24px; }

.site-title {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info { font-size: 14px; }

.btn-link {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  text-decoration: underline;
}

.main-container {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
}

.sidebar {
  width: 220px;
  background: white;
  border-right: 1px solid #ddd;
  min-height: calc(100vh - 60px);
  padding: 16px 0;
}

.nav-list {
  list-style: none;
}

.nav-link {
  display: block;
  padding: 12px 20px;
  color: #333;
  text-decoration: none;
  border-left: 3px solid transparent;
}

.nav-link:hover, .nav-link.active {
  background: #e8f0fe;
  border-left-color: #1a4d8c;
  color: #1a4d8c;
}

.content {
  flex: 1;
  padding: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1a4d8c;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  padding: 20px;
  border: 1px solid #ddd;
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a4d8c;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}

.section {
  background: white;
  border: 1px solid #ddd;
  padding: 20px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.data-table th {
  background: #f8f9fa;
  font-weight: 600;
  font-size: 13px;
  color: #555;
}

.btn {
  padding: 6px 12px;
  border: none;
  cursor: pointer;
  font-size: 12px;
  margin-right: 4px;
}

.btn-primary { background: #1a4d8c; color: white; }
.btn-secondary { background: #6c757d; color: white; }
.btn-sm { padding: 4px 8px; }

.service-status {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-indicator.online { background: #28a745; }
.status-indicator.offline { background: #dc3545; }
</style>
