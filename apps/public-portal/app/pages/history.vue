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
        <h2 class="page-title">Lịch sử hoạt động</h2>

        <div class="filter-bar">
          <select v-model="filter" class="select">
            <option value="all">Tất cả</option>
            <option value="sign">Chỉ ký</option>
            <option value="verify">Chỉ xác thực</option>
          </select>
          <input type="date" v-model="dateFrom" class="input-date" />
          <span>đến</span>
          <input type="date" v-model="dateTo" class="input-date" />
        </div>

        <div v-if="loading" class="loading">Đang tải...</div>

        <div v-else class="section">
          <table class="data-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Hoạt động</th>
                <th>Văn bản</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredActivities" :key="item.id">
                <td>{{ item.time }}</td>
                <td>{{ item.type === 'sign' ? 'Ký văn bản' : 'Xác thực' }}</td>
                <td>{{ item.documentName }}</td>
                <td><span class="status-badge" :class="item.status.toLowerCase()">{{ item.statusText }}</span></td>
              </tr>
            </tbody>
          </table>

          <div v-if="filteredActivities.length === 0" class="empty-state">
            <p>Không có hoạt động nào phù hợp với bộ lọc.</p>
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
const filter = ref('all')
const dateFrom = ref('')
const dateTo = ref('')

const activities = ref([])

onMounted(() => {
  setTimeout(() => {
    activities.value = [
      { id: '1', type: 'sign', documentName: 'Hop_dong_2024.pdf', time: '25/12/2024 14:32', status: 'success', statusText: 'Hoàn thành' },
      { id: '2', type: 'verify', documentName: 'Giay_phep_kinh_doanh.pdf', time: '24/12/2024 10:15', status: 'success', statusText: 'Hợp lệ' },
      { id: '3', type: 'sign', documentName: 'Bao_cao_tai_chinh_Q4.xlsx', time: '23/12/2024 09:00', status: 'success', statusText: 'Hoàn thành' },
      { id: '4', type: 'verify', documentName: 'Chung_nhan_dau_tu.pdf', time: '22/12/2024 16:45', status: 'error', statusText: 'Không hợp lệ' },
      { id: '5', type: 'sign', documentName: 'Don_de_nghi.docx', time: '21/12/2024 11:20', status: 'pending', statusText: 'Đang chờ' },
    ]
    loading.value = false
  }, 300)
})

const filteredActivities = computed(() => {
  if (filter.value === 'all') return activities.value
  return activities.value.filter(a => a.type === filter.value)
})
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
.container { max-width: 1000px; margin: 0 auto; }
.page-title { font-size: 20px; font-weight: 600; margin-bottom: 24px; color: #1a4d8c; }
.filter-bar { display: flex; gap: 12px; align-items: center; margin-bottom: 20px; background: white; padding: 12px 16px; border: 1px solid #ddd; }
.select, .input-date { padding: 8px 12px; border: 1px solid #ddd; font-size: 14px; }
.loading { text-align: center; padding: 40px; color: #666; }
.section { background: white; border: 1px solid #ddd; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #eee; }
.data-table th { background: #f8f9fa; font-weight: 600; font-size: 13px; color: #555; }
.status-badge { display: inline-block; padding: 3px 8px; font-size: 12px; }
.status-badge.success { background: #d4edda; color: #155724; }
.status-badge.pending { background: #fff3cd; color: #856404; }
.status-badge.error { background: #f8d7da; color: #721c24; }
.empty-state { text-align: center; padding: 40px; color: #666; }
.gov-footer { background: #333; color: #ccc; text-align: center; padding: 16px; font-size: 13px; }
</style>
