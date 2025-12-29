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
        const response = await $fetch<{ sessionId?: string; username: string; message: string; token?: string; user?: User }>(`${apiBase.value}/auth/login`, {
            method: 'POST',
            body: { username, password },
            credentials: 'include' // Include cookies for session-based auth
        })

        // Handle both session-based and JWT-based responses
        if (response.token) {
            // JWT-based auth
            token.value = response.token
            try {
                const payload = JSON.parse(atob(response.token.split('.')[1]))
                user.value = {
                    ...response.user,
                    role: payload.role || 'CITIZEN'
                } as User
            } catch {
                user.value = response.user || { username: response.username, role: 'CITIZEN' }
            }
            if (import.meta.client) {
                localStorage.setItem('token', response.token)
                localStorage.setItem('user', JSON.stringify(user.value))
            }
        } else if (response.sessionId) {
            // Session-based auth (cookie-managed)
            user.value = {
                username: response.username,
                role: 'CITIZEN' // Default role, can be fetched separately
            }
            token.value = response.sessionId // Use sessionId as token identifier
            if (import.meta.client) {
                localStorage.setItem('sessionId', response.sessionId)
                localStorage.setItem('user', JSON.stringify(user.value))
            }
        } else {
            throw new Error('Invalid login response')
        }

        return response
    }

    const logout = () => {
        token.value = null
        user.value = null
        if (import.meta.client) {
            localStorage.removeItem('token')
            localStorage.removeItem('sessionId')
            localStorage.removeItem('user')
        }
    }

    const checkAuth = () => {
        if (import.meta.client) {
            const storedToken = localStorage.getItem('token') || localStorage.getItem('sessionId')
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
