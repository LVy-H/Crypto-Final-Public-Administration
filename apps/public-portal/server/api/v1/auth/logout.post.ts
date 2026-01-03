/**
 * Proxy logout request to api-gateway
 */
export default defineEventHandler(async (event) => {
    const apiBase = process.env.NUXT_UPSTREAM_API_URL || 'http://api-gateway:8080'

    try {
        const response = await $fetch(`${apiBase}/api/v1/auth/logout`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        return response
    } catch (error: any) {
        // Logout errors are usually non-critical
        return { message: 'Logged out' }
    }
})
