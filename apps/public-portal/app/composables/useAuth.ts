import { ref, computed } from 'vue'

interface User {
    username: string
    email?: string
    role: 'CITIZEN' | 'ADMIN' | 'OFFICER'
    fullName?: string
}

const user = ref<User | null>(null)
const token = ref<string | null>(null)

export function useAuth() {
    const isAuthenticated = computed(() => !!token.value)
    const isAdmin = computed(() => user.value?.role === 'ADMIN')
    const isCitizen = computed(() => user.value?.role === 'CITIZEN')
    const isOfficer = computed(() => user.value?.role === 'OFFICER')
    const hasAdminAccess = computed(() => isAdmin.value || isOfficer.value)

    const config = useRuntimeConfig()
    const apiBase = computed(() => config.public.apiBase || 'http://localhost:8080/api/v1')

    const login = async (username: string, password: string) => {
        const response = await $fetch<{ token: string; user: User }>(`${apiBase.value}/auth/login`, {
            method: 'POST',
            body: { username, password }
        })

        token.value = response.token
        user.value = response.user

        if (import.meta.client) {
            localStorage.setItem('token', response.token)
            localStorage.setItem('user', JSON.stringify(response.user))
        }

        return response
    }

    const logout = () => {
        token.value = null
        user.value = null
        if (import.meta.client) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
        }
    }

    const checkAuth = () => {
        if (import.meta.client) {
            const storedToken = localStorage.getItem('token')
            const storedUser = localStorage.getItem('user')

            if (storedToken && storedUser) {
                token.value = storedToken
                try {
                    user.value = JSON.parse(storedUser)
                } catch {
                    logout()
                }
            }
        }
        return isAuthenticated.value
    }

    return {
        user,
        token,
        isAuthenticated,
        isAdmin,
        isCitizen,
        isOfficer,
        hasAdminAccess,
        login,
        logout,
        checkAuth
    }
}
