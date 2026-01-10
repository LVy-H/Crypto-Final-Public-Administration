<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Certificate {
  id: string
  serialNumber: string
  subject: string
  algorithm: string
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED'
  validUntil: string
}

const certificates = ref<Certificate[]>([])
const loading = ref(true)

function loadCertificates() {
  loading.value = true
  // Mock GET /api/v1/certificates/my
  setTimeout(() => {
    certificates.value = [
      {
        id: 'uuid-1',
        serialNumber: '547823901238',
        subject: 'CN=Nguyen Van A, O=Gov, C=VN',
        algorithm: 'ML-DSA-44',
        status: 'ACTIVE',
        validUntil: '2026-12-31'
      },
      {
        id: 'uuid-2',
        serialNumber: '123890123890',
        subject: 'CN=Nguyen Van A (Signer), O=Gov, C=VN',
        algorithm: 'ML-DSA-65',
        status: 'REVOKED',
        validUntil: '2025-06-30'
      }
    ]
    loading.value = false
  }, 800)
}

function requestNewCert() {
  alert('Called POST /api/v1/certificates/request')
  loadCertificates() // Refresh mock
}

function downloadCert(id: string) {
  alert(`Called GET /api/v1/certificates/${id}/download`)
}

onMounted(() => {
  loadCertificates()
})
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h2>Quản lý Chứng thư số</h2>
      <button @click="requestNewCert" class="btn-new">+ Yêu cầu mới</button>
    </div>

    <div v-if="loading" class="loading">Đang tải dữ liệu...</div>

    <table v-else class="cert-table">
      <thead>
        <tr>
          <th>Serial Number</th>
          <th>Chủ sở hữu (Subject)</th>
          <th>Thuật toán</th>
          <th>Trạng thái</th>
          <th>Hết hạn</th>
          <th>Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="cert in certificates" :key="cert.id">
          <td>{{ cert.serialNumber }}</td>
          <td>{{ cert.subject }}</td>
          <td><span class="tag-algo">{{ cert.algorithm }}</span></td>
          <td>
            <span :class="['status', cert.status.toLowerCase()]">{{ cert.status }}</span>
          </td>
          <td>{{ cert.validUntil }}</td>
          <td>
            <button @click="downloadCert(cert.id)" class="btn-sm">Tải về</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page-container { max-width: 1000px; margin: 2rem auto; padding: 1rem; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
h2 { color: #1a4d8c; }
.btn-new { background: #1a4d8c; color: white; border: none; padding: 0.6rem 1.2rem; border-radius: 4px; cursor: pointer; }
.cert-table { width: 100%; border-collapse: collapse; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.cert-table th, .cert-table td { padding: 1rem; text-align: left; border-bottom: 1px solid #eee; }
.cert-table th { background: #f8f9fa; font-weight: 600; color: #555; }
.status { padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.85rem; font-weight: 500; }
.status.active { background: #d4edda; color: #155724; }
.status.revoked { background: #f8d7da; color: #721c24; }
.tag-algo { background: #e2e3e5; padding: 0.2rem 0.4rem; border-radius: 3px; font-size: 0.8rem; font-family: monospace; }
.btn-sm { padding: 0.3rem 0.6rem; border: 1px solid #ccc; background: white; cursor: pointer; border-radius: 3px; }
.btn-sm:hover { background: #f0f0f0; }
.loading { text-align: center; padding: 2rem; color: #666; }
</style>
