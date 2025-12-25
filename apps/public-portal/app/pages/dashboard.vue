<template>
  <div class="dashboard-page">
    <header class="gov-header">
      <div class="header-content">
        <div class="header-left">
          <span class="logo">🏛️</span>
          <h1>Hệ thống Chữ ký số Lượng tử</h1>
        </div>
        <div class="header-right">
          <span class="user-info">Xin chào, {{ user?.username || 'Người dùng' }}</span>
          <button @click="logout" class="btn-link">Đăng xuất</button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <h2 class="page-title">Bảng điều khiển</h2>

        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalSigned }}</div>
            <div class="stat-label">Văn bản đã ký</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.verified }}</div>
            <div class="stat-label">Đã xác thực</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">Đang chờ xử lý</div>
          </div>
        </div>

        <div class="actions-section">
          <h3>Chức năng chính</h3>
          <div class="action-buttons">
            <NuxtLink to="/sign/upload" class="action-btn">
              <span class="action-icon">📝</span>
              <span>Ký văn bản</span>
            </NuxtLink>
            <NuxtLink to="/verify" class="action-btn">
              <span class="action-icon">🔍</span>
              <span>Xác thực chữ ký</span>
            </NuxtLink>
            <NuxtLink to="/certificates" class="action-btn">
              <span class="action-icon">📜</span>
              <span>Chứng chỉ của tôi</span>
            </NuxtLink>
            <NuxtLink to="/history" class="action-btn">
              <span class="action-icon">📋</span>
              <span>Lịch sử hoạt động</span>
            </NuxtLink>
          </div>
        </div>

        <div class="cert-section">
          <h3>Thông tin chứng chỉ</h3>
          <table class="info-table">
            <tr>
              <th>Thuật toán</th>
              <td>ML-DSA-44 (Dilithium2)</td>
            </tr>
            <tr>
              <th>Trạng thái</th>
              <td><span class="status-badge active">Hoạt động</span></td>
            </tr>
            <tr>
              <th>Ngày cấp</th>
              <td>{{ certInfo.issued }}</td>
            </tr>
            <tr>
              <th>Ngày hết hạn</th>
              <td>{{ certInfo.expires }}</td>
            </tr>
          </table>
        </div>

        <div class="activity-section">
          <h3>Hoạt động gần đây</h3>
          <table class="data-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Hoạt động</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in recentActivity" :key="item.id">
                <td>{{ item.time }}</td>
                <td>{{ item.title }}</td>
                <td><span :class="['status-badge', item.status]">{{ item.statusText }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>

    <footer class="gov-footer">
      <p>© 2024 Hệ thống Chữ ký số Lượng tử - Nguyễn Trọng Nhân & Lê Việt Hoàng</p>
    </footer>
  </div>
</template>

<script setup>
const router = useRouter()

const user = ref(null)
const stats = ref({ totalSigned: 12, verified: 10, pending: 2 })
const certInfo = ref({ issued: '15/01/2024', expires: '15/01/2025' })

const recentActivity = ref([
  { id: 1, time: '25/12/2024 14:32', title: 'Ký văn bản: Hop_dong_2024.pdf', status: 'success', statusText: 'Hoàn thành' },
  { id: 2, time: '25/12/2024 10:15', title: 'Xác thực: Bao_cao_Q4.pdf', status: 'success', statusText: 'Hợp lệ' },
  { id: 3, time: '24/12/2024 16:45', title: 'Gửi yêu cầu gia hạn chứng chỉ', status: 'pending', statusText: 'Đang chờ' },
])

onMounted(() => {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return
  }
  
  const userData = localStorage.getItem('user')
  if (userData) {
    user.value = JSON.parse(userData)
  }
})

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.gov-header {
  background: #1a4d8c;
  color: white;
  padding: 12px 24px;
  border-bottom: 3px solid #c41e3a;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo { font-size: 24px; }

.gov-header h1 {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
}

.btn-link {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  text-decoration: underline;
}

.main-content {
  flex: 1;
  padding: 24px;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1a4d8c;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border: 1px solid #ddd;
  padding: 20px;
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a4d8c;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}

.actions-section, .cert-section, .activity-section {
  background: white;
  border: 1px solid #ddd;
  padding: 20px;
  margin-bottom: 24px;
}

.actions-section h3, .cert-section h3, .activity-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
  color: #333;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  text-decoration: none;
  color: #333;
  font-size: 13px;
}

.action-btn:hover {
  background: #e8f0fe;
  border-color: #1a4d8c;
}

.action-icon { font-size: 24px; }

.info-table {
  width: 100%;
}

.info-table th, .info-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.info-table th {
  width: 150px;
  color: #666;
  font-weight: 500;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th, .data-table td {
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

.status-badge {
  display: inline-block;
  padding: 3px 8px;
  font-size: 12px;
}

.status-badge.active, .status-badge.success {
  background: #d4edda;
  color: #155724;
}

.status-badge.pending {
  background: #fff3cd;
  color: #856404;
}

.gov-footer {
  background: #333;
  color: #ccc;
  text-align: center;
  padding: 16px;
  font-size: 13px;
}
</style>
