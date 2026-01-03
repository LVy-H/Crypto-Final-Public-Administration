<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'

definePageMeta({
  middleware: 'auth'
})

const {
  generateKey,
  listKeys,
  initSigning,
  confirmSigning,
  hashDocument,
  readFileAsBuffer,
  createKeyAlias,
  files,
  openFileDialog,
  resetFiles,
  copyToClipboard,
  copied
} = useSigningService()
const { getStatus: getTotpStatus } = useTotp()
const toast = useToast()

// OTP schema
const otpSchema = z.object({
  otp: z.string().length(6, 'OTP phải có đúng 6 số').regex(/^\d+$/, 'OTP chỉ chứa số')
})

type OtpSchema = z.output<typeof otpSchema>

// State
const selectedFile = ref<File | null>(null)
const selectedKeyAlias = ref('')
const challengeId = ref('')
const showOtpModal = ref(false)
const otpState = reactive({ otp: '' })
const signatureResult = ref<any>(null)
const creatingKey = ref(false)
const signing = ref(false)
const totpEnabled = ref(false)

// Keys data
const keysData = ref<any[]>([])
const keysLoading = ref(true)

// Fetch keys on mount
async function fetchKeys() {
  keysLoading.value = true
  try {
    const result = await listKeys()
    keysData.value = result || []
    if (result?.length && !selectedKeyAlias.value) {
      selectedKeyAlias.value = result[0].alias
    }
  } catch {
    keysData.value = []
  } finally {
    keysLoading.value = false
  }
}

const hasKey = computed(() => keysData.value.length > 0)

// Key items for select
const keyItems = computed(() =>
  keysData.value.map(k => ({ label: k.alias, value: k.alias }))
)

// Watch file selection from VueUse
watch(files, (newFiles) => {
  if (newFiles && newFiles.length > 0) {
    selectedFile.value = newFiles[0]
    signatureResult.value = null
  }
})

// Format file size
const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// Create new signing key
async function createKey() {
  creatingKey.value = true
  try {
    const result = await generateKey('mldsa65')
    selectedKeyAlias.value = result.alias || createKeyAlias()
    await fetchKeys()
    toast.add({ title: 'Tạo khóa thành công', color: 'success' })
  } catch (e: any) {
    toast.add({
      title: 'Lỗi tạo khóa',
      description: e.data?.error || e.message,
      color: 'error'
    })
  } finally {
    creatingKey.value = false
  }
}

// Start signing process
async function startSigning() {
  if (!selectedFile.value || !selectedKeyAlias.value) return

  signing.value = true
  try {
    const buffer = await readFileAsBuffer(selectedFile.value)
    const hash = await hashDocument(buffer)
    const challenge = await initSigning(selectedKeyAlias.value, hash)
    challengeId.value = challenge.challengeId
    showOtpModal.value = true
  } catch (e: any) {
    toast.add({
      title: 'Lỗi khởi tạo ký',
      description: e.data?.error || e.message,
      color: 'error'
    })
  } finally {
    signing.value = false
  }
}

// Confirm signing with OTP
async function onConfirmSign(event: FormSubmitEvent<OtpSchema>) {
  signing.value = true
  try {
    const result = await confirmSigning(challengeId.value, event.data.otp)
    signatureResult.value = result
    showOtpModal.value = false
    otpState.otp = ''
    toast.add({ title: 'Ký thành công!', color: 'success' })
  } catch (e: any) {
    toast.add({
      title: 'Xác nhận thất bại',
      description: e.data?.error || 'Mã OTP không hợp lệ',
      color: 'error'
    })
  } finally {
    signing.value = false
  }
}

// Download signature
function downloadSignature() {
  if (!signatureResult.value) return
  const blob = new Blob([signatureResult.value.signatureBase64], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `signature-${Date.now()}.sig`
  a.click()
  URL.revokeObjectURL(url)
}

// Initialize on mount
onMounted(async () => {
  await fetchKeys()
  const status = await getTotpStatus()
  totpEnabled.value = status.enabled
})
</script>

<template>
  <div class="max-w-2xl mx-auto p-6 space-y-6">
    <!-- Page Header -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Ký tài liệu số</h1>
      <p class="mt-1 text-gray-500 dark:text-gray-400">
        Ký điện tử với thuật toán ML-DSA (Post-Quantum)
      </p>
    </div>

    <!-- Key Status Card -->
    <UCard>
      <div class="flex justify-between items-center">
        <div>
          <div class="flex items-center gap-2">
            <UIcon name="i-lucide-key" class="text-primary" />
            <h3 class="font-semibold text-gray-900 dark:text-white">Khóa ký của bạn</h3>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            <template v-if="keysLoading">Đang tải...</template>
            <template v-else-if="hasKey">Đang sử dụng: {{ selectedKeyAlias }}</template>
            <template v-else>Bạn cần tạo khóa ký trước khi ký tài liệu</template>
          </p>
        </div>

        <UButton v-if="!hasKey && !keysLoading" @click="createKey" :loading="creatingKey">
          Tạo khóa ký
        </UButton>

        <USelect
          v-else-if="hasKey"
          v-model="selectedKeyAlias"
          :items="keyItems"
          class="w-48"
        />
      </div>
    </UCard>

    <!-- TOTP Status -->
    <UCard>
      <div class="flex justify-between items-center">
        <div>
          <div class="flex items-center gap-2">
            <UIcon name="i-lucide-shield-check" class="text-primary" />
            <h3 class="font-semibold text-gray-900 dark:text-white">Xác thực 2 bước (TOTP)</h3>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            {{ totpEnabled ? 'TOTP đã được kích hoạt' : 'Kích hoạt TOTP để bảo mật cao hơn' }}
          </p>
        </div>

        <NuxtLink :to="'/sign/setup-totp'">
          <UButton :color="totpEnabled ? 'neutral' : 'primary'" variant="soft">
            {{ totpEnabled ? 'Quản lý TOTP' : 'Kích hoạt TOTP' }}
          </UButton>
        </NuxtLink>
      </div>
    </UCard>

    <!-- Signing Section -->
    <UCard v-if="hasKey">
      <h3 class="font-semibold text-gray-900 dark:text-white mb-4">Chọn tài liệu để ký</h3>

      <!-- File Upload Area -->
      <div
        class="border-2 border-dashed border-gray-300 dark:border-gray-700 rounded-lg p-8 text-center cursor-pointer hover:border-primary transition-colors"
        @click="openFileDialog()"
      >
        <UIcon name="i-lucide-file-up" class="text-4xl text-gray-400 mb-2" />
        <p class="text-gray-600 dark:text-gray-300">
          Kéo thả hoặc <strong class="text-primary">chọn file</strong>
        </p>
        <p class="text-sm text-gray-400 mt-1">PDF, DOC, DOCX, TXT</p>
      </div>

      <!-- Selected File -->
      <div v-if="selectedFile" class="flex items-center gap-3 mt-4 p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
        <UIcon name="i-lucide-file" class="text-primary text-xl" />
        <div class="flex-1 min-w-0">
          <p class="font-medium text-gray-900 dark:text-white truncate">{{ selectedFile.name }}</p>
          <p class="text-sm text-gray-500">{{ formatFileSize(selectedFile.size) }}</p>
        </div>
        <UButton
          icon="i-lucide-x"
          variant="ghost"
          color="neutral"
          @click="selectedFile = null; resetFiles()"
        />
      </div>

      <!-- Sign Button -->
      <UButton
        v-if="selectedFile"
        block
        size="lg"
        class="mt-4"
        :loading="signing"
        @click="startSigning"
      >
        Ký tài liệu
      </UButton>
    </UCard>

    <!-- Signature Result -->
    <UAlert v-if="signatureResult" color="success" icon="i-lucide-check-circle">
      <template #title>Ký thành công!</template>
      <template #description>
        <div class="space-y-2">
          <p><strong>Thuật toán:</strong> {{ signatureResult.signatureAlgorithm || 'ML-DSA-65' }}</p>
          <p><strong>Thời gian:</strong> {{ new Date().toLocaleString('vi-VN') }}</p>
          <div class="flex gap-2 mt-3">
            <UButton size="xs" variant="soft" @click="downloadSignature">
              Tải chữ ký
            </UButton>
            <UButton size="xs" variant="soft" @click="copyToClipboard(signatureResult.signatureBase64)">
              {{ copied ? 'Đã copy!' : 'Copy chữ ký' }}
            </UButton>
          </div>
        </div>
      </template>
    </UAlert>

    <!-- OTP Modal -->
    <UModal v-model:open="showOtpModal" title="Nhập mã OTP" description="Nhập mã 6 số từ ứng dụng authenticator">
      <UButton label="Trigger" class="hidden" />

      <template #body>
        <UForm :schema="otpSchema" :state="otpState" @submit="onConfirmSign">
          <UFormField name="otp" class="mb-4">
            <UInput
              v-model="otpState.otp"
              type="text"
              maxlength="6"
              placeholder="000000"
              class="text-center text-2xl tracking-widest"
            />
          </UFormField>

          <div class="flex gap-2">
            <UButton variant="ghost" block @click="showOtpModal = false">
              Hủy
            </UButton>
            <UButton type="submit" block :loading="signing" :disabled="otpState.otp?.length !== 6">
              Xác nhận
            </UButton>
          </div>
        </UForm>
      </template>
    </UModal>
  </div>
</template>
