/**
 * Proxy login request to api-gateway and set user session
 */
export default defineEventHandler(async (event) => {
    const body = await readBody(event)
    const apiBase = process.env.NUXT_UPSTREAM_API_URL || 'http://api-gateway:8080'

    try {
        const response = await $fetch<{
            username: string
            email?: string
            role?: string
            token?: string
            sessionId?: string
        }>(`${apiBase}/api/v1/auth/login`, {
            method: 'POST',
            body,
            headers: {
                'Content-Type': 'application/json'
            }
        })

        // Set user session using nuxt-auth-utils
        await setUserSession(event, {
            user: {
                username: response.username,
                email: response.email,
                role: response.role || 'CITIZEN'
            },
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
