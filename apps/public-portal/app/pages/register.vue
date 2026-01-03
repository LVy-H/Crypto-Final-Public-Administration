<script setup lang="ts">
import { z } from 'zod'
import type { FormSubmitEvent, FormErrorEvent } from '@nuxt/ui'

// Redirect if already logged in
const { loggedIn } = useUserSession()
if (loggedIn.value) {
  navigateTo('/dashboard')
}

const { register } = useAuth()
const toast = useToast()

// Zod schema with password confirmation
const schema = z.object({
  username: z.string().min(3, 'Tên đăng nhập phải có ít nhất 3 ký tự').max(50),
  email: z.string().email('Email không hợp lệ'),
  password: z.string()
    .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
    .regex(/[A-Z]/, 'Mật khẩu phải có ít nhất 1 chữ hoa')
    .regex(/[a-z]/, 'Mật khẩu phải có ít nhất 1 chữ thường')
    .regex(/[0-9]/, 'Mật khẩu phải có ít nhất 1 số'),
  confirmPassword: z.string()
}).refine(data => data.password === data.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword']
})

type Schema = z.infer<typeof schema>

const state = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})
const loading = ref(false)

async function onSubmit(event: FormSubmitEvent<Schema>) {
  loading.value = true
  try {
    await register({
      username: event.data.username,
      email: event.data.email,
      password: event.data.password
    })
    toast.add({ title: 'Đăng ký thành công', color: 'success' })
    await navigateTo('/dashboard')
  } catch (e: any) {
    toast.add({
      title: 'Lỗi đăng ký',
      description: e.data?.message || 'Không thể đăng ký tài khoản',
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
  <div class="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4 py-8">
    <UCard class="w-full max-w-md">
      <template #header>
        <div class="text-center">
          <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Đăng ký tài khoản</h1>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Tạo tài khoản để sử dụng dịch vụ chữ ký số
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
          />
        </UFormField>

        <UFormField label="Email" name="email" required>
          <UInput
            v-model="state.email"
            type="email"
            name="email"
            placeholder="email@example.com"
            icon="i-lucide-mail"
          />
        </UFormField>

        <UFormField label="Mật khẩu" name="password" required>
          <UInput
            v-model="state.password"
            type="password"
            name="password"
            placeholder="Ít nhất 8 ký tự"
            icon="i-lucide-lock"
          />
        </UFormField>

        <UFormField label="Xác nhận mật khẩu" name="confirmPassword" required>
          <UInput
            v-model="state.confirmPassword"
            type="password"
            name="confirmPassword"
            placeholder="Nhập lại mật khẩu"
            icon="i-lucide-lock"
          />
        </UFormField>

        <UButton type="submit" block size="lg" :loading="loading">
          Đăng ký
        </UButton>
        </UForm>

      <template #footer>
        <p class="text-center text-sm text-gray-500 dark:text-gray-400">
          Đã có tài khoản?
          <NuxtLink to="/login" class="text-primary font-medium">
            Đăng nhập
          </NuxtLink>
        </p>
      </template>
    </UCard>
  </div>
</template>
