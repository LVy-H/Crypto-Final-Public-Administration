<template>

    <div class="page-content">
      <h2 class="page-title">Chứng chỉ của tôi</h2>

      <div v-if="loading" class="loading">Đang tải...</div>

      <template v-else>
        <div v-for="cert in certificates" :key="cert.id" class="section">
          <div class="cert-header">
            <h3>{{ cert.subject }}</h3>
            <span class="status-badge" :class="cert.status?.toLowerCase()">{{ cert.statusText }}</span>
          </div>
          <table class="info-table">
            <tr><th>Thuật toán</th><td>{{ cert.algorithm }}</td></tr>
            <tr><th>Ngày cấp</th><td>{{ cert.issuedAt }}</td></tr>
            <tr><th>Ngày hết hạn</th><td>{{ cert.expiresAt }}</td></tr>
            <tr><th>Số serial</th><td class="mono">{{ cert.serialNumber }}</td></tr>
          </table>
          <div class="cert-actions">
            <button @click="downloadCert(cert)" class="btn">📥 Tải xuống</button>
          </div>
        </div>

        <div v-if="certificates.length === 0" class="empty-state">
          <p>Bạn chưa có chứng chỉ nào.</p>
        </div>

        <div class="section">
          <h3>Yêu cầu chứng chỉ mới</h3>
          <div class="request-form">
            <select v-model="newCertAlgorithm" class="select">
              <option value="ML-DSA-44">ML-DSA-44 (Tiêu chuẩn)</option>
              <option value="ML-DSA-65">ML-DSA-65 (Bảo mật cao)</option>
              <option value="ML-DSA-87">ML-DSA-87 (Bảo mật tối đa)</option>
            </select>
            <button @click="requestCertificate" class="btn btn-primary" :disabled="requesting">
              {{ requesting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
            </button>
          </div>
        </div>
      </template>
    </div>

</template>

<script setup>
definePageMeta({ middleware: 'auth' })

const config = useRuntimeConfig()
const { token } = useAuth()

const loading = ref(true)
const certificates = ref([])
const newCertAlgorithm = ref('ML-DSA-44')
const requesting = ref(false)

const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

onMounted(async () => {
  try {
    const authToken = token.value || localStorage.getItem('token')
    const res = await fetch(`${apiBase.value}/certificates/my`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    if (res.ok) {
      const data = await res.json()
      certificates.value = data.map(c => ({
        ...c,
        statusText: c.revoked ? 'Thu hồi' : 'Hoạt động',
        status: c.revoked ? 'revoked' : 'active',
        issuedAt: new Date(c.notBefore).toLocaleDateString('vi-VN'),
        expiresAt: new Date(c.notAfter).toLocaleDateString('vi-VN')
      }))
    }
  } catch (e) {
    console.error('Error loading certificates:', e)
  } finally {
    loading.value = false
  }
})

function downloadCert(cert) {
  alert('Tải xuống chứng chỉ: ' + cert.serialNumber)
}

async function requestCertificate() {
  requesting.value = true
  try {
    const authToken = token.value || localStorage.getItem('token')
    const res = await fetch(`${apiBase.value}/certificates/request`, {
      method: 'POST',
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ algorithm: newCertAlgorithm.value })
    })
    if (res.ok) {
      alert('Yêu cầu đã được gửi!')
    } else {
      alert('Có lỗi xảy ra')
    }
  } catch (e) {
    alert('Lỗi kết nối')
  } finally {
    requesting.value = false
  }
}
</script>

<style scoped>
.page-content { max-width: 800px; }
.page-title { font-size: 1.25rem; color: #1a4d8c; margin-bottom: 1.5rem; }
.loading { padding: 2rem; text-align: center; color: #666; }

.section { background: white; border: 1px solid #ddd; padding: 1.25rem; margin-bottom: 1rem; }
.section h3 { font-size: 0.95rem; font-weight: 600; margin-bottom: 0.75rem; }

.cert-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.cert-header h3 { margin: 0; font-size: 0.9rem; }

.status-badge { padding: 0.2rem 0.5rem; font-size: 0.75rem; border-radius: 3px; }
.status-badge.active { background: #d4edda; color: #155724; }
.status-badge.revoked { background: #f8d7da; color: #721c24; }

.info-table { width: 100%; margin-bottom: 1rem; }
.info-table th, .info-table td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
.info-table th { width: 120px; color: #666; font-weight: 500; }
.info-table .mono { font-family: monospace; font-size: 0.8rem; }

.cert-actions { display: flex; gap: 0.5rem; }

.btn { padding: 0.5rem 0.75rem; border: 1px solid #ddd; background: white; font-size: 0.8rem; cursor: pointer; }
.btn:hover { background: #f5f5f5; }
.btn-primary { background: #1a4d8c; color: white; border-color: #1a4d8c; }
.btn-primary:hover { background: #153d6e; }
.btn-primary:disabled { background: #999; cursor: not-allowed; }

.empty-state { background: white; border: 1px solid #ddd; padding: 2rem; text-align: center; color: #666; margin-bottom: 1rem; }

.request-form { display: flex; gap: 0.75rem; }
.select { flex: 1; padding: 0.5rem; border: 1px solid #ddd; font-size: 0.85rem; }
</style>
