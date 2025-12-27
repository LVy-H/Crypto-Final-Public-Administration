<template>
  <div class="page">
    <header class="gov-header">
      <div class="header-content">
        <div class="header-left">
          <span class="logo">🏛️</span>
          <h1>Hệ thống Chữ ký số Lượng tử</h1>
        </div>
        <div class="header-right">
          <NuxtLink to="/dashboard" class="btn-link">← Bảng điều khiển</NuxtLink>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <h2 class="page-title">Chứng chỉ của tôi</h2>

        <div v-if="loading" class="loading">Đang tải...</div>

        <div v-else>
          <div class="section" v-for="cert in certificates" :key="cert.id">
            <div class="cert-header">
              <h3>{{ cert.subject }}</h3>
              <span class="status-badge" :class="cert.status.toLowerCase()">{{ cert.statusText }}</span>
            </div>
            <table class="info-table">
              <tr><th>Thuật toán</th><td>{{ cert.algorithm }}</td></tr>
              <tr><th>Ngày cấp</th><td>{{ cert.issuedAt }}</td></tr>
              <tr><th>Ngày hết hạn</th><td>{{ cert.expiresAt }}</td></tr>
              <tr><th>Số serial</th><td class="mono">{{ cert.serialNumber }}</td></tr>
            </table>
            <div class="cert-actions">
              <button @click="downloadCert(cert)" class="btn-secondary">📥 Tải xuống</button>
              <button @click="viewDetails(cert)" class="btn-secondary">ℹ️ Chi tiết</button>
            </div>
          </div>

          <div v-if="certificates.length === 0" class="empty-state">
            <p>Bạn chưa có chứng chỉ nào.</p>
            <NuxtLink to="/register" class="btn-primary">Yêu cầu chứng chỉ</NuxtLink>
          </div>
        </div>

        <div class="section">
          <h3>Yêu cầu chứng chỉ mới</h3>
          <p class="hint">Tạo cặp khóa lượng tử mới và yêu cầu cấp chứng chỉ</p>
          <div class="request-form">
            <select v-model="newCertAlgorithm" class="select">
              <option value="ML-DSA-44">ML-DSA-44 (Dilithium2) - Tiêu chuẩn</option>
              <option value="ML-DSA-65">ML-DSA-65 (Dilithium3) - Bảo mật cao</option>
              <option value="ML-DSA-87">ML-DSA-87 (Dilithium5) - Bảo mật tối đa</option>
            </select>
            <button @click="requestCertificate" class="btn-primary" :disabled="requesting">
              {{ requesting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
            </button>
          </div>
        </div>
      </div>
    </main>

    <footer class="gov-footer">
      <p>© 2024 Hệ thống Chữ ký số Lượng tử - Nguyễn Trọng Nhân & Lê Việt Hoàng</p>
    </footer>
  </div>
</template>

<script setup>
const loading = ref(true)
const certificates = ref([])
const newCertAlgorithm = ref('ML-DSA-44')
const requesting = ref(false)

onMounted(() => {
  setTimeout(() => {
    certificates.value = [
      { id: '1', subject: 'CN=testcitizen, O=Chính phủ Việt Nam', algorithm: 'ML-DSA-44', status: 'active', statusText: 'Hoạt động', issuedAt: '15/01/2024', expiresAt: '15/01/2025', serialNumber: 'A1B2C3D4E5F67890' }
    ]
    loading.value = false
  }, 300)
})

function downloadCert(cert) { alert('Tải xuống chứng chỉ: ' + cert.subject) }
function viewDetails(cert) { alert('Xem chi tiết: ' + cert.serialNumber) }

async function requestCertificate() {
  requesting.value = true
  await new Promise(r => setTimeout(r, 1000))
  alert('Yêu cầu đã được gửi! Đang chờ phê duyệt.')
  requesting.value = false
}
</script>

<style scoped>
.page { min-height: 100vh; display: flex; flex-direction: column; background: #f5f5f5; }
.gov-header { background: #1a4d8c; color: white; padding: 12px 24px; border-bottom: 3px solid #c41e3a; }
.header-content { display: flex; justify-content: space-between; align-items: center; max-width: 1200px; margin: 0 auto; }
.header-left { display: flex; align-items: center; gap: 12px; }
.logo { font-size: 24px; }
.gov-header h1 { font-size: 18px; font-weight: 600; }
.btn-link { color: white; text-decoration: underline; background: none; border: none; cursor: pointer; }
.main-content { flex: 1; padding: 24px; }
.container { max-width: 800px; margin: 0 auto; }
.page-title { font-size: 20px; font-weight: 600; margin-bottom: 24px; color: #1a4d8c; }
.loading { text-align: center; padding: 40px; color: #666; }
.section { background: white; border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; }
.section h3 { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #333; }
.cert-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.cert-header h3 { margin: 0; }
.status-badge { padding: 4px 10px; font-size: 12px; font-weight: 600; }
.status-badge.active { background: #d4edda; color: #155724; }
.status-badge.revoked { background: #f8d7da; color: #721c24; }
.status-badge.expired { background: #fff3cd; color: #856404; }
.info-table { width: 100%; margin-bottom: 16px; }
.info-table th, .info-table td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #eee; }
.info-table th { width: 140px; color: #666; font-weight: 500; }
.info-table .mono { font-family: monospace; font-size: 13px; }
.cert-actions { display: flex; gap: 8px; }
.btn-secondary { background: #f8f9fa; border: 1px solid #ddd; padding: 8px 16px; font-size: 13px; cursor: pointer; }
.btn-secondary:hover { background: #e9ecef; }
.empty-state { text-align: center; padding: 40px; background: white; border: 1px solid #ddd; }
.hint { font-size: 13px; color: #666; margin-bottom: 16px; }
.request-form { display: flex; gap: 12px; }
.select { flex: 1; padding: 10px; border: 1px solid #ddd; font-size: 14px; }
.btn-primary { background: #1a4d8c; color: white; border: none; padding: 10px 20px; font-size: 14px; cursor: pointer; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }
.gov-footer { background: #333; color: #ccc; text-align: center; padding: 16px; font-size: 13px; }
</style>
