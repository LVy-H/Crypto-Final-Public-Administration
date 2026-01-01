/**
 * Authentication actions composable
 * Provides login, register, and logout functionality
 */
export interface User {
    username: string
    email?: string
    role: 'CITIZEN' | 'ADMIN' | 'OFFICER'
    fullName?: string
}

interface LoginResponse {
    sessionId?: string
    token?: string
    username: string
    message: string
    user?: User
}

interface RegisterForm {
    username: string
    email: string
    password: string
    role?: string
}

export const useAuth = () => {
    const { post } = useApi()
    const authState = useAuthState()

    /**
     * Login with username and password
     */
    const login = async (username: string, password: string) => {
        const response = await post<LoginResponse>('/auth/login', { username, password })

        // Determine token (session-based or JWT)
        const token = response.sessionId || response.token
        if (!token) {
            throw new Error('Invalid login response: no token received')
        }

        // Build user object
        let user: User
        if (response.token) {
            // JWT-based auth - decode payload
            try {
                const payload = JSON.parse(atob(response.token.split('.')[1]))
                user = {
                    ...response.user,
                    username: response.username,
                    role: payload.role || 'CITIZEN'
                } as User
            } catch {
                user = response.user || { username: response.username, role: 'CITIZEN' }
            }
        } else {
            // Session-based auth
            user = {
                ...response.user,
                username: response.username,
                role: response.user?.role || 'CITIZEN'
            } as User
        }

        authState.setAuth(token, user)
        return response
    }

    /**
     * Register a new user and auto-login
     */
    const register = async (form: RegisterForm) => {
        await post('/auth/register', {
            username: form.username,
            email: form.email,
            password: form.password,
            role: form.role || 'USER'
        })

        // Auto-login after registration
        return login(form.username, form.password)
    }

    /**
     * Check if user is authenticated (for middleware compatibility)
     */
    const checkAuth = () => {
        // On client, try to restore from storage if not already authenticated
        if (import.meta.client && !authState.isAuthenticated.value) {
            authState.restoreFromStorage()
        }
        return authState.isAuthenticated.value
    }

    return {
        // Re-export state
        user: authState.user,
        token: authState.token,
        isAuthenticated: authState.isAuthenticated,
        isAdmin: authState.isAdmin,
        isOfficer: authState.isOfficer,
        isCitizen: authState.isCitizen,
        hasAdminAccess: authState.hasAdminAccess,
        // Actions
        login,
        register,
        logout: authState.logout,
        checkAuth
    }
}
