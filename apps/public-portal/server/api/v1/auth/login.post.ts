/**
 * Proxy login request to api-gateway and set user session
 * Backend returns sessionId as a token for subsequent API calls
 */
export default defineEventHandler(async (event) => {
    const body = await readBody(event)
    const apiBase = process.env.NUXT_UPSTREAM_API_URL || 'http://api-gateway:8080'

    try {
        const response = await $fetch<{
            user?: { username: string; email?: string; role?: string }
            username: string
            email?: string
            role?: string
            sessionId?: string
            message?: string
        }>(`${apiBase}/api/v1/auth/login`, {
            method: 'POST',
            body,
            headers: {
                'Content-Type': 'application/json'
            }
        })

        // Get username from response (may be nested or at top level)
        const username = response.user?.username || response.username
        const email = response.user?.email || response.email
        const role = response.user?.role || response.role || 'CITIZEN'

        // Set user session - store sessionId for backend API calls
        await setUserSession(event, {
            user: {
                username,
                email,
                role
            },
            sessionId: response.sessionId, // Store session token for backend API calls
            loggedInAt: Date.now()
        })

        return response
    } catch (error: any) {
        console.error('Login proxy error:', error.message || error)
        throw createError({
            statusCode: error.statusCode || 401,
            message: error.data?.message || error.message || 'Login failed'
        })
    }
})
