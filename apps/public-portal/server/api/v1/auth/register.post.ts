/**
 * Proxy register request to api-gateway
 */
export default defineEventHandler(async (event) => {
    const body = await readBody(event)
    const apiBase = process.env.NUXT_UPSTREAM_API_URL || 'http://api-gateway:8080'

    try {
        const response = await $fetch(`${apiBase}/api/v1/auth/register`, {
            method: 'POST',
            body,
            headers: {
                'Content-Type': 'application/json'
            }
        })
        return response
    } catch (error: any) {
        console.error('Register proxy error:', error.message || error)
        throw createError({
            statusCode: error.statusCode || 500,
            message: error.data?.message || error.message || 'Registration failed'
        })
    }
})
