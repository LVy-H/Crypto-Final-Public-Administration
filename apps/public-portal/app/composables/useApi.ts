/**
 * Centralized API composable
 * Uses credentials: 'include' for session cookie forwarding
 */
export interface ApiResponse<T> {
    data: T
    status: number
    ok: boolean
}

export const useApi = () => {
    const config = useRuntimeConfig()
    const baseURL = config.public.apiBase || '/api/v1'

    /**
     * Base fetch with authentication and error handling
     */
    const apiFetch = async <T>(
        endpoint: string,
        options: RequestInit & { body?: unknown } = {}
    ): Promise<T> => {
        const { body, headers: customHeaders, ...rest } = options

        return await $fetch<T>(`${baseURL}${endpoint}`, {
            ...rest,
            body: body as BodyInit,
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...customHeaders
            },
            onResponseError({ response }) {
                if (response.status === 401) {
                    navigateTo('/login')
                }
            }
        })
    }

    /**
     * Reactive data fetching with useFetch
     */
    const useApiData = <T>(
        endpoint: string | (() => string),
        options: Parameters<typeof useFetch>[1] = {}
    ) => {
        return useFetch<T>(endpoint, {
            baseURL,
            credentials: 'include',
            ...options,
            onResponseError({ response }) {
                if (response.status === 401) {
                    navigateTo('/login')
                }
            }
        })
    }

    // Convenience methods
    const get = <T>(endpoint: string) =>
        apiFetch<T>(endpoint, { method: 'GET' })

    const post = <T>(endpoint: string, body?: unknown) =>
        apiFetch<T>(endpoint, { method: 'POST', body })

    const put = <T>(endpoint: string, body?: unknown) =>
        apiFetch<T>(endpoint, { method: 'PUT', body })

    const del = <T>(endpoint: string) =>
        apiFetch<T>(endpoint, { method: 'DELETE' })

    return {
        apiFetch,
        useApiData,
        get,
        post,
        put,
        del,
        baseURL
    }
}
