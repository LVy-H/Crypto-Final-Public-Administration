<script setup lang="ts">
definePageMeta({
  middleware: 'auth'
})

const { user, loggedIn } = useUserSession()
const { get } = useApi()

const loading = ref(true)
const stats = ref({ signedDocuments: 0, certificates: 0, pendingRequests: 0 })
const certInfo = ref<any>(null)
const recentActivity = ref<any[]>([])

// Quick action items
const quickActions = [
  { to: '/sign', icon: 'i-lucide-pen-tool', label: 'Ký văn bản', color: 'primary' },
  { to: '/verify', icon: 'i-lucide-shield-check', label: 'Xác thực', color: 'success' },
  { to: '/certificates', icon: 'i-lucide-award', label: 'Chứng chỉ', color: 'info' },
  { to: '/history', icon: 'i-lucide-history', label: 'Lịch sử', color: 'warning' }
]

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return isNaN(date.getTime()) ? dateStr : date.toLocaleString('vi-VN')
}

const formatAction = (action: string) => {
  const labels: Record<string, string> = {
    'LOGIN': 'Đăng nhập',
    'LOGOUT': 'Đăng xuất',
    'SIGN_DOCUMENT': 'Ký tài liệu',
    'VERIFY_DOCUMENT': 'Xác thực tài liệu',
    'REQUEST_CERTIFICATE': 'Yêu cầu chứng chỉ',
    'GENERATE_KEY': 'Tạo khóa'
  }
  return labels[action] || action
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    'active': 'success',
    'success': 'success',
    'completed': 'success',
    'pending': 'warning',
    'revoked': 'error',
    'failed': 'error'
  }
  return colors[status?.toLowerCase()] || 'neutral'
}

const loadDashboard = async () => {
  loading.value = true
  try {
    // Load stats
    try {
      stats.value = await get('/user/stats')
    } catch { /* Stats not available */ }

    // Load certificate info
    try {
      const certs = await get<any[]>('/certificates/my')
      certInfo.value = certs?.[0] || null
    } catch { /* Certs not available */ }

    // Load recent activity
    try {
      recentActivity.value = await get<any[]>('/user/activity?limit=5') || []
    } catch { /* Activity not available */ }
  } catch (e) {
    console.error('Dashboard load error:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="max-w-5xl mx-auto p-6">
    <!-- Page Header -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        Xin chào, {{ user?.username || 'User' }}
      </h1>
      <p class="mt-1 text-gray-500 dark:text-gray-400">
        Tổng quan hoạt động của bạn
      </p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-12">
      <UIcon name="i-lucide-loader-2" class="text-4xl text-primary animate-spin" />
      <p class="mt-2 text-gray-500">Đang tải...</p>
    </div>

    <template v-else>
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <UCard>
          <div class="text-center">
            <div class="text-4xl font-bold text-primary">{{ stats.signedDocuments || 0 }}</div>
            <div class="text-sm text-gray-500 mt-1">Văn bản đã ký</div>
          </div>
        </UCard>
        <UCard>
          <div class="text-center">
            <div class="text-4xl font-bold text-primary">{{ stats.certificates || 0 }}</div>
            <div class="text-sm text-gray-500 mt-1">Chứng chỉ</div>
          </div>
        </UCard>
        <UCard>
          <div class="text-center">
            <div class="text-4xl font-bold text-primary">{{ stats.pendingRequests || 0 }}</div>
            <div class="text-sm text-gray-500 mt-1">Chờ xử lý</div>
          </div>
        </UCard>
      </div>

      <!-- Quick Actions -->
      <UCard class="mb-8">
        <template #header>
          <h3 class="font-semibold text-gray-900 dark:text-white">Chức năng</h3>
        </template>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <NuxtLink
            v-for="action in quickActions"
            :key="action.to"
            :to="action.to"
            class="flex flex-col items-center gap-2 p-4 rounded-lg border border-gray-200 dark:border-gray-700 hover:border-primary hover:bg-primary-50 dark:hover:bg-primary-900/10 transition-all"
          >
            <UIcon :name="action.icon" class="text-3xl text-primary" />
            <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ action.label }}</span>
          </NuxtLink>
        </div>
      </UCard>

      <!-- Certificate Info -->
      <UCard v-if="certInfo" class="mb-8">
        <template #header>
          <h3 class="font-semibold text-gray-900 dark:text-white">Chứng chỉ của bạn</h3>
        </template>

        <div class="grid grid-cols-3 gap-4 text-sm">
          <div>
            <span class="text-gray-500">Thuật toán</span>
            <p class="font-medium text-gray-900 dark:text-white">{{ certInfo.algorithm || 'ML-DSA-65' }}</p>
          </div>
          <div>
            <span class="text-gray-500">Trạng thái</span>
            <p>
              <UBadge :color="getStatusColor(certInfo.status)">
                {{ certInfo.status || 'Hoạt động' }}
              </UBadge>
            </p>
          </div>
          <div>
            <span class="text-gray-500">Hết hạn</span>
            <p class="font-medium text-gray-900 dark:text-white">{{ formatDate(certInfo.expiresAt) }}</p>
          </div>
        </div>
      </UCard>

      <!-- Recent Activity -->
      <UCard>
        <template #header>
          <h3 class="font-semibold text-gray-900 dark:text-white">Hoạt động gần đây</h3>
        </template>

        <UTable
          v-if="recentActivity.length > 0"
          :data="recentActivity"
          :columns="[
            { key: 'timestamp', label: 'Thời gian' },
            { key: 'action', label: 'Hoạt động' },
            { key: 'status', label: 'Trạng thái' }
          ]"
        >
          <template #timestamp-cell="{ row }">
            {{ formatDate(row.timestamp || row.createdAt) }}
          </template>
          <template #action-cell="{ row }">
            {{ formatAction(row.action) }}
          </template>
          <template #status-cell="{ row }">
            <UBadge :color="getStatusColor(row.status)">
              {{ row.status }}
            </UBadge>
          </template>
        </UTable>

        <div v-else class="text-center py-8 text-gray-500">
          Chưa có hoạt động nào
        </div>
      </UCard>
    </template>
  </div>
</template>
