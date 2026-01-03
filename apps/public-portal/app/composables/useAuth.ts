/**
 * Authentication composable using nuxt-auth-utils
 * Provides login, register, logout with session management
 */
export interface UserData {
    username: string
    email?: string
    role?: 'CITIZEN' | 'ADMIN' | 'OFFICER'
    fullName?: string
}

interface LoginResponse {
    sessionId?: string
    token?: string
    username: string
    message: string
    user?: UserData
}

export const useAuth = () => {
    const { loggedIn, user, session, fetch: refreshSession, clear } = useUserSession()

    // Backward compatibility aliases
    const isAuthenticated = loggedIn
    const token = computed(() => session.value?.sessionId || null)

    /**
     * Check auth (no-op, session is restored automatically)
     */
    const checkAuth = () => {
        return loggedIn.value
    }

    /**
     * Login with username and password
     */
    const login = async (username: string, password: string) => {
        const response = await $fetch<LoginResponse>('/api/v1/auth/login', {
            method: 'POST',
            body: { username, password },
            credentials: 'include'
        })

        // Refresh session to sync state with cookies
        await refreshSession()
        return response
    }

    /**
     * Register a new user
     */
    const register = async (form: { username: string; email: string; password: string }) => {
        await $fetch('/api/v1/auth/register', {
            method: 'POST',
            body: form,
            credentials: 'include'
        })

        // Auto-login after registration
        return login(form.username, form.password)
    }

    /**
     * Logout and clear session
     */
    const logout = async () => {
        try {
            await $fetch('/api/v1/auth/logout', {
                method: 'POST',
                credentials: 'include'
            })
        } catch {
            // Ignore logout errors
        }
        await clear()
        await navigateTo('/login')
    }

    // Role-based computed properties
    const isAdmin = computed(() => (user.value as UserData | null)?.role === 'ADMIN')
    const isOfficer = computed(() => (user.value as UserData | null)?.role === 'OFFICER')
    const isCitizen = computed(() => (user.value as UserData | null)?.role === 'CITIZEN')
    const hasAdminAccess = computed(() => isAdmin.value || isOfficer.value)

    return {
        // Session state from nuxt-auth-utils
        user: user as Ref<UserData | null>,
        loggedIn,
        session,
        refreshSession,
        // Backward compatibility
        isAuthenticated,
        token,
        checkAuth,
        // Computed roles
        isAdmin,
        isOfficer,
        isCitizen,
        hasAdminAccess,
        // Actions
        login,
        register,
        logout
    }
}
