/**
 * Centralized API composable for all backend requests
 * Automatically includes auth headers and handles errors
 */
export const useApi = () => {
    const config = useRuntimeConfig()
    const { token, logout } = useAuthState()

    const baseURL = computed(() => config.public.apiBase || '/api/v1')

    /**
     * Make an authenticated API request using $fetch
     */
    const request = async <T>(
        endpoint: string,
        options: {
            method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
            body?: Record<string, unknown>
            headers?: Record<string, string>
        } = {}
    ): Promise<T> => {
        const { method = 'GET', body, headers = {} } = options

        // Build auth header - fallback to localStorage if reactive token not available
        const authToken = token.value || (import.meta.client ? localStorage.getItem('sessionId') : null)
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`
        } else {
            console.warn(`[useApi] No token found for ${endpoint}`)
        }

        console.log(`[useApi] Requesting ${endpoint} with token: ${authToken ? 'YES' : 'NO'}`)

        try {
            return await $fetch<T>(`${baseURL.value}${endpoint}`, {
                method,
                body,
                headers: {
                    'Content-Type': 'application/json',
                    ...headers
                },
                credentials: 'include'
            })
        } catch (error: unknown) {
            // Handle auth errors globally - only logout on 401 (unauthorized)
            // 403 (forbidden) should be handled by the caller (may be permission issue, not auth)
            if (error && typeof error === 'object' && 'statusCode' in error) {
                const statusCode = (error as { statusCode: number }).statusCode
                if (statusCode === 401) {
                    logout()
                    navigateTo('/login')
                }
            }
            throw error
        }
    }

    // Convenience methods
    const get = <T>(endpoint: string, headers?: Record<string, string>) =>
        request<T>(endpoint, { method: 'GET', headers })

    const post = <T>(endpoint: string, body?: Record<string, unknown>, headers?: Record<string, string>) =>
        request<T>(endpoint, { method: 'POST', body, headers })

    const put = <T>(endpoint: string, body?: Record<string, unknown>, headers?: Record<string, string>) =>
        request<T>(endpoint, { method: 'PUT', body, headers })

    const del = <T>(endpoint: string, headers?: Record<string, string>) =>
        request<T>(endpoint, { method: 'DELETE', headers })

    return {
        request,
        get,
        post,
        put,
        del,
        baseURL
    }
}
