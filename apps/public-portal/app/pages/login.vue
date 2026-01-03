<script setup lang="ts">
import { z } from 'zod'
import type { FormSubmitEvent, FormErrorEvent } from '@nuxt/ui'

// Redirect if already logged in
const { loggedIn } = useUserSession()
if (loggedIn.value) {
  navigateTo('/dashboard')
}

const { login } = useAuth()
const toast = useToast()

// Zod schema for validation
const schema = z.object({
  username: z.string().min(3, 'Tên đăng nhập phải có ít nhất 3 ký tự'),
  password: z.string().min(6, 'Mật khẩu phải có ít nhất 6 ký tự')
})

type Schema = z.infer<typeof schema>

const state = reactive({
  username: '',
  password: ''
})
const loading = ref(false)



async function onSubmit(event: FormSubmitEvent<Schema>) {
  loading.value = true
  try {
    await login(event.data.username, event.data.password)
    toast.add({ title: 'Đăng nhập thành công', color: 'success' })
    await navigateTo('/dashboard')
  } catch (e: any) {
    toast.add({
      title: 'Lỗi đăng nhập',
      description: e.data?.message || 'Tên đăng nhập hoặc mật khẩu không đúng',
      color: 'error'
    })
  } finally {
    loading.value = false
  }
}

function onError(event: FormErrorEvent) {
  console.error('Validation errors:', event.errors)
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
    <UCard class="w-full max-w-md">
      <template #header>
        <div class="text-center">
          <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Đăng nhập</h1>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Hệ thống Chữ ký số Hậu lượng tử
          </p>
        </div>
      </template>

      <UForm :schema="schema" :state="state" class="space-y-4" @submit="onSubmit" @error="onError">
        <UFormField label="Tên đăng nhập" name="username" required>
          <UInput

            v-model="state.username"
            name="username"
            placeholder="Nhập tên đăng nhập"
            icon="i-lucide-user"
            size="lg"
          />
        </UFormField>

        <UFormField label="Mật khẩu" name="password" required>
          <UInput
            v-model="state.password"
            type="password"
            name="password"
            placeholder="Nhập mật khẩu"
            icon="i-lucide-lock"
            size="lg"
          />
        </UFormField>

        <UButton type="submit" block size="lg" :loading="loading">
          Đăng nhập
        </UButton>
        </UForm>

      <template #footer>
        <p class="text-center text-sm text-gray-500 dark:text-gray-400">
          Chưa có tài khoản?
          <NuxtLink to="/register" class="text-primary font-medium">
            Đăng ký ngay
          </NuxtLink>
        </p>
      </template>
    </UCard>
  </div>
</template>
