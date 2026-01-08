// Proxy GET /api/v1/user/activity to api-gateway
// Forwards sessionId as Authorization Bearer token
export default defineEventHandler(async (event) => {
    const config = useRuntimeConfig()
    const upstreamUrl = config.upstreamApiUrl || 'http://api-gateway:8080'

    // Get session to access the sessionId token
    const session = await getUserSession(event)
    const sessionId = session?.sessionId as string | undefined

    // Get query params
    const query = getQuery(event)
    const queryString = new URLSearchParams(query as Record<string, string>).toString()
    const url = queryString
        ? `${upstreamUrl}/api/v1/user/activity?${queryString}`
        : `${upstreamUrl}/api/v1/user/activity`

    try {
        const response = await $fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                // Forward sessionId as Authorization Bearer token
                ...(sessionId ? {
                    'Authorization': `Bearer ${sessionId}`,
                    'X-Auth-Token': sessionId
                } : {})
            }
        })
        return response
    } catch (error: any) {
        console.error('User activity error:', error.data || error.message)
        throw createError({
            statusCode: error.statusCode || 500,
            message: error.data?.message || 'Failed to fetch user activity'
        })
    }
})
