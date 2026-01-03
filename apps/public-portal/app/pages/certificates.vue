<template>
  <div class="certificates-page">
    <div class="page-header">
      <h1>Chứng chỉ số của tôi</h1>
      <NuxtLink to="/certificates/request" class="btn btn-primary">+ Yêu cầu chứng chỉ mới</NuxtLink>
    </div>

    <div v-if="loading" class="loading">Đang tải...</div>

    <div v-else-if="certificates.length === 0" class="empty-state">
      <div class="empty-icon">📜</div>
      <h3>Chưa có chứng chỉ</h3>
      <p>Bạn chưa có chứng chỉ số nào. Hãy yêu cầu chứng chỉ để bắt đầu ký tài liệu.</p>
      <NuxtLink to="/certificates/request" class="btn btn-primary">Yêu cầu chứng chỉ</NuxtLink>
    </div>

    <div v-else class="certificates-list">
      <div v-for="cert in certificates" :key="cert.id" class="certificate-card">
        <div class="cert-header">
          <span class="cert-status" :class="cert.status.toLowerCase()">{{ statusLabel(cert.status) }}</span>
          <span class="cert-algorithm">{{ cert.algorithm }}</span>
        </div>
        <div class="cert-body">
          <h3>{{ cert.subjectName || 'Chứng chỉ ' + cert.id.substring(0, 8) }}</h3>
          <div class="cert-details">
            <p><strong>Serial:</strong> {{ cert.serialNumber }}</p>
            <p><strong>Issuer:</strong> {{ cert.issuerName }}</p>
            <p><strong>Hiệu lực:</strong> {{ formatDate(cert.validFrom) }} - {{ formatDate(cert.validTo) }}</p>
          </div>
        </div>
        <div class="cert-actions">
          <button @click="downloadCert(cert.id)" class="btn btn-secondary">Tải xuống</button>
          <NuxtLink :to="`/certificates/${cert.id}`" class="btn btn-outline">Chi tiết</NuxtLink>
        </div>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
definePageMeta({
  middleware: 'auth'
})

const { getMyCertificates, downloadCertificate } = useCertificates()

const loading = ref(true)
const certificates = ref([])
const error = ref('')

const statusLabel = (status) => {
  const labels = {
    'PENDING': 'Đang chờ',
    'ACTIVE': 'Hoạt động',
    'REVOKED': 'Đã thu hồi',
    'EXPIRED': 'Hết hạn'
  }
  return labels[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A'
  return new Date(dateStr).toLocaleDateString('vi-VN')
}

const fetchCertificates = async () => {
  try {
    loading.value = true
    certificates.value = await getMyCertificates()
  } catch (e) {
    error.value = 'Không thể tải danh sách chứng chỉ'
  } finally {
    loading.value = false
  }
}

const downloadCert = async (id) => {
  try {
    const result = await downloadCertificate(id)
    // Create download
    const blob = new Blob([result.certificatePem], { type: 'application/x-pem-file' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `certificate-${id}.pem`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    error.value = 'Không thể tải chứng chỉ'
  }
}

onMounted(fetchCertificates)
</script>

<style scoped>
.certificates-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 1.5rem;
  color: #1a4d8c;
}

.loading {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #333;
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: #666;
  margin-bottom: 1.5rem;
}

.certificates-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.certificate-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.25rem;
}

.cert-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.cert-status {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
}

.cert-status.active { background: #e6f4ea; color: #137333; }
.cert-status.pending { background: #fef7e0; color: #b06000; }
.cert-status.revoked { background: #fce8e6; color: #c5221f; }
.cert-status.expired { background: #f1f3f4; color: #5f6368; }

.cert-algorithm {
  font-size: 0.8rem;
  color: #1a4d8c;
  font-weight: 500;
}

.cert-body h3 {
  font-size: 1rem;
  margin-bottom: 0.5rem;
}

.cert-details {
  font-size: 0.85rem;
  color: #666;
}

.cert-details p {
  margin: 0.25rem 0;
}

.cert-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  text-decoration: none;
}

.btn-primary { background: #1a4d8c; color: white; border: none; }
.btn-secondary { background: #f5f5f5; color: #333; border: 1px solid #ddd; }
.btn-outline { background: transparent; color: #1a4d8c; border: 1px solid #1a4d8c; }

.error-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fce8e6;
  color: #c5221f;
  border-radius: 4px;
}
</style>
