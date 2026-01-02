<template>
  <div class="admin-page">
    <div class="page-header">
      <h2 class="page-title">Quản lý Cấu trúc PKI</h2>
      <button class="btn btn-primary" @click="showCreateModal = true">Thêm CA Mới</button>
    </div>

    <!-- Stats Overview -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ rootCas.length }}</div>
        <div class="stat-label">Root/National CA</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ provincialCas.length }}</div>
        <div class="stat-label">Provincial CAs</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ districtRas.length }}</div>
        <div class="stat-label">District RAs</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ pendingRequests.length }}</div>
        <div class="stat-label">Chờ duyệt</div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="section">
      <div class="tabs">
        <button 
          v-for="tab in ['ALL', 'root', 'issuing', 'ra', 'pending']" 
          :key="tab"
          class="tab-btn" 
          :class="{ active: currentTab === tab }"
          @click="currentTab = tab"
        >
          {{ tabLabels[tab] }}
          <span v-if="tab === 'pending' && pendingRequests.length > 0" class="badge badge-pending ml-2">{{ pendingRequests.length }}</span>
        </button>
      </div>

      <!-- Active CAs Table -->
      <table v-if="currentTab !== 'pending'" class="data-table">
        <thead>
          <tr>
            <th>Tên CA</th>
            <th>Loại</th>
            <th>Cấp độ</th>
            <th>Thuật toán</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredCas.length === 0">
            <td colspan="6" class="text-center">Không có dữ liệu</td>
          </tr>
          <tr v-for="ca in filteredCas" :key="ca.id">
            <td class="ca-name">
                <span class="tree-indicator" :style="{ marginLeft: (ca.level * 20) + 'px' }">
                   {{ ca.level > 0 ? '└─' : '' }} 
                </span>
                {{ ca.name }}
            </td>
            <td>{{ formatType(ca.type) }}</td>
            <td>{{ ca.level }}</td>
            <td>{{ ca.algorithm }}</td>
            <td><span :class="['badge', 'badge-' + ca.status.toLowerCase()]">{{ ca.status }}</span></td>
            <td>
              <button class="btn btn-sm" @click="viewDetails(ca)">Chi tiết</button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pending Requests Table -->
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Ngày tạo</th>
            <th>Tên Yêu Cầu</th>
            <th>Loại</th>
            <th>Thuật toán</th>
            <th>Người yêu cầu</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="pendingRequests.length === 0">
            <td colspan="7" class="text-center">Không có yêu cầu nào đang chờ duyệt</td>
          </tr>
          <tr v-for="req in pendingRequests" :key="req.id">
            <td>{{ new Date(req.createdAt).toLocaleDateString() }}</td>
            <td class="ca-name">{{ req.name }}</td>
            <td>{{ formatType(req.type) }}</td>
            <td>{{ req.algorithm }}</td>
            <td>{{ req.requesterUsername }}</td>
            <td><span class="badge badge-pending">PENDING</span></td>
            <td class="actions-cell">
              <button class="btn btn-sm btn-success mr-2" @click="approveRequest(req.id)" :disabled="processing === req.id">
                {{ processing === req.id ? '...' : 'Duyệt' }}
              </button>
              <button class="btn btn-sm btn-danger" @click="rejectRequest(req.id)" :disabled="processing === req.id">
                {{ processing === req.id ? '...' : 'Từ chối' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create (Request) Modal -->
    <div v-if="showCreateModal" class="modal-overlay">
      <div class="modal">
        <h3>Gửi Yêu Cầu Tạo CA / RA Mới</h3>
        
        <form @submit.prevent="createCaRequest">
          <div class="form-group">
            <label>Tên CA / RA</label>
            <input v-model="newCa.name" required placeholder="Ví dụ: Hai Ba Trung District RA" />
          </div>

          <div class="form-group">
            <label>Parent CA (Cấp trên)</label>
            <select v-model="newCa.parentId" required>
               <option v-for="ca in issuingCas" :key="ca.id" :value="ca.id">
                 {{ ca.name }} ({{ ca.algorithm }})
               </option>
            </select>
          </div>

          <div class="form-group">
            <label>Loại</label>
            <select v-model="newCa.type" required>
              <option value="ISSUING_CA">Issuing CA (Cấp Tỉnh/Bộ)</option>
              <option value="RA">Registration Authority (Cấp Quận/Huyện)</option>
            </select>
          </div>

          <div class="form-group">
            <label>Thuật toán (PQC ML-DSA)</label>
            <select v-model="newCa.algorithm">
              <option value="mldsa87">ML-DSA-87 (High Security)</option>
              <option value="mldsa65">ML-DSA-65 (Standard)</option>
              <option value="mldsa44">ML-DSA-44 (Fast)</option>
            </select>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="showCreateModal = false">Hủy</button>
            <button type="submit" class="btn btn-primary" :disabled="creating">
              {{ creating ? 'Đang gửi...' : 'Gửi Yêu Cầu' }}
            </button>
          </div>
        </form>
      </div>
    </div>

  </div>
</template>

<script setup>
definePageMeta({
  layout: 'admin',
  middleware: 'auth'
})

const { get, post } = useApi()

const cas = ref([])
const pendingRequests = ref([])
const showCreateModal = ref(false)
const creating = ref(false)
const processing = ref(null) // ID of request being processed
const currentTab = ref('ALL')

const newCa = ref({
  name: '',
  parentId: '',
  type: 'ISSUING_CA',
  algorithm: 'mldsa65'
})

const tabLabels = {
  ALL: 'Tất cả CA',
  root: 'Root CA',
  issuing: 'Issuing CA',
  ra: 'RA',
  pending: 'Yêu cầu chờ duyệt'
}

// Computed
const rootCas = computed(() => cas.value.filter(c => c.level === 0 || c.type === 'ROOT_CA'))
const provincialCas = computed(() => cas.value.filter(c => c.level === 1 || c.type === 'ISSUING_CA'))
const districtRas = computed(() => cas.value.filter(c => c.level === 2 || c.type === 'RA'))

// For sorting/display logic - sort by level then parent
const sortedCas = computed(() => {
    return [...cas.value].sort((a, b) => {
        if (a.level !== b.level) return a.level - b.level
        return a.name.localeCompare(b.name)
    })
})

const filteredCas = computed(() => {
  if (currentTab.value === 'ALL') return sortedCas.value
  if (currentTab.value === 'root') return cas.value.filter(c => c.level === 0)
  if (currentTab.value === 'issuing') return cas.value.filter(c => c.type === 'ISSUING_CA')
  if (currentTab.value === 'ra') return cas.value.filter(c => c.type === 'RA' || c.type === 'EXTERNAL_RA')
  // Pending is handled separately in template
  return []
})

const issuingCas = computed(() => cas.value.filter(c => c.type === 'ISSUING_CA' || c.level === 0 || c.type === 'ROOT_CA'))

const formatType = (type) => {
    const map = {
        'ROOT_CA': 'Root CA',
        'ISSUING_CA': 'Issuing CA',
        'RA': 'RA',
        'EXTERNAL_RA': 'External RA'
    }
    return map[type] || type
}

// Methods
const loadData = async () => {
  try {
    const [casData, requestsData] = await Promise.all([
        get('/ca/all'),
        get('/ca-management/requests/pending')
    ])
    
    cas.value = casData || []
    pendingRequests.value = requestsData || []

    // Pre-select first parent if available
    if (issuingCas.value.length > 0 && !newCa.value.parentId) {
        newCa.value.parentId = issuingCas.value[0].id
    }
  } catch (e) {
    console.error("Load CA error", e)
  }
}

const createCaRequest = async () => {
    if (!newCa.value.name || !newCa.value.parentId) return
    creating.value = true
    try {
        await post(`/ca-management/${newCa.value.parentId}/request`, {
            name: newCa.value.name,
            type: newCa.value.type,
            algorithm: newCa.value.algorithm,
            label: newCa.value.type === 'RA' ? 'District RA' : 'Subordinate CA',
            validDays: newCa.value.type === 'RA' ? 730 : 1825
        })
        alert('Yêu cầu tạo CA đã được gửi và đang chờ duyệt.')
        showCreateModal.value = false
        newCa.value.name = '' // Reset
        loadData()
        currentTab.value = 'pending' // Switch to pending tab
    } catch(e) {
        alert('Lỗi: ' + (e.message || 'Unknown'))
    } finally {
        creating.value = false
    }
}

const approveRequest = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn DUYỆT yêu cầu này? Hành động này sẽ tạo và ký chứng thư số.')) return
    processing.value = id
    try {
        await post(`/ca-management/requests/${id}/approve`, {})
        alert('Đã duyệt yêu cầu thành công!')
        loadData()
    } catch (e) {
        alert('Lỗi duyệt yêu cầu: ' + (e.message || 'Unknown'))
    } finally {
        processing.value = null
    }
}

const rejectRequest = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn TỪ CHỐI yêu cầu này?')) return
    processing.value = id
    try {
        await post(`/ca-management/requests/${id}/reject`, {})
        alert('Đã từ chối yêu cầu.')
        loadData()
    } catch (e) {
        alert('Lỗi từ chối yêu cầu: ' + (e.message || 'Unknown'))
    } finally {
        processing.value = null
    }
}

const viewDetails = (ca) => {
    alert(`Chi tiết CA:\nID: ${ca.id}\nParent: ${ca.parentCaId}\nStatus: ${ca.status}`)
}

onMounted(() => {
    loadData()
})
</script>

<style scoped>
.admin-page { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
.page-title { font-size: 1.5rem; color: #1a4d8c; margin: 0; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 2rem; }
.stat-card { background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); text-align: center; }
.stat-value { font-size: 2rem; font-weight: 700; color: #1a4d8c; margin-bottom: 0.5rem; }
.stat-label { color: #666; font-size: 0.9rem; }

.section { background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }

.tabs { margin-bottom: 1.5rem; border-bottom: 1px solid #eee; padding-bottom: 1rem; }
.tab-btn { background: none; border: none; padding: 0.5rem 1rem; margin-right: 0.5rem; cursor: pointer; color: #666; font-weight: 500; border-radius: 4px; }
.tab-btn.active { background: #e8f0fe; color: #1a4d8c; }

.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 1rem; text-align: left; border-bottom: 1px solid #eee; }
.data-table th { font-weight: 600; color: #444; background: #f9f9f9; }

.tree-indicator { color: #999; margin-right: 0.5rem; display: inline-block; }
.ca-name { font-weight: 500; }

.badge { padding: 0.25rem 0.6rem; border-radius: 12px; font-size: 0.75rem; font-weight: 600; }
.badge-active { background: #e6f4ea; color: #1e7e34; }
.badge-revoked { background: #fce8e6; color: #c5221f; }
.badge-pending { background: #fff8e1; color: #f9a825; }

.btn { padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; font-weight: 500; }
.btn-sm { padding: 0.3rem 0.6rem; font-size: 0.8rem; background: #f1f3f4; color: #333; }
.btn-primary { background: #1a4d8c; color: white; }
.btn-secondary { background: #f1f3f4; color: #333; }
.btn-success { background: #e6f4ea; color: #1e7e34; }
.btn-danger { background: #fce8e6; color: #c5221f; }

.mr-2 { margin-right: 0.5rem; }
.ml-2 { margin-left: 0.5rem; }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; padding: 2rem; border-radius: 8px; width: 100%; max-width: 500px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.modal h3 { margin-top: 0; color: #1a4d8c; }

.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.5rem; color: #444; font-weight: 500; }
.form-group input, .form-group select { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; font-size: 1rem; }

.form-actions { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1.5rem; }
</style>
