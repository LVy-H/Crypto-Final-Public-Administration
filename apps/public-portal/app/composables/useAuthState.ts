import type { User } from './useAuth'

/**
 * SSR-safe authentication state composable using Nuxt's useState
 * This composable manages the reactive auth state that persists across SSR/client hydration
 */
export const useAuthState = () => {
    // Use useState for SSR-safe reactive state (shared across all components)
    const user = useState<User | null>('auth-user', () => null)
    const token = useState<string | null>('auth-token', () => null)

    // Computed properties
    const isAuthenticated = computed(() => !!token.value && !!user.value)
    const isAdmin = computed(() => user.value?.role === 'ADMIN')
    const isOfficer = computed(() => user.value?.role === 'OFFICER')
    const isCitizen = computed(() => user.value?.role === 'CITIZEN')
    const hasAdminAccess = computed(() => isAdmin.value || isOfficer.value)

    /**
     * Set authentication state and persist to localStorage
     */
    const setAuth = (newToken: string, newUser: User) => {
        token.value = newToken
        user.value = newUser
        if (import.meta.client) {
            localStorage.setItem('sessionId', newToken)
            localStorage.setItem('user', JSON.stringify(newUser))
        }
    }

    /**
     * Clear authentication state and localStorage
     */
    const logout = () => {
        token.value = null
        user.value = null
        if (import.meta.client) {
            localStorage.removeItem('sessionId')
            localStorage.removeItem('token')
            localStorage.removeItem('user')
        }
    }

    /**
     * Restore auth state from localStorage (called by client plugin)
     */
    const restoreFromStorage = () => {
        if (!import.meta.client) return false

        const storedToken = localStorage.getItem('sessionId') || localStorage.getItem('token')
        const storedUser = localStorage.getItem('user')

        if (storedToken && storedUser) {
            try {
                token.value = storedToken
                user.value = JSON.parse(storedUser)
                return true
            } catch {
                logout()
                return false
            }
        }
        return false
    }

    return {
        user: readonly(user),
        token: readonly(token),
        isAuthenticated,
        isAdmin,
        isOfficer,
        isCitizen,
        hasAdminAccess,
        setAuth,
        logout,
        restoreFromStorage
    }
}
