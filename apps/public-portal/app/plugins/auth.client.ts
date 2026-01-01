/**
 * Client-only plugin to restore auth state from localStorage on hydration
 * This runs only on the client after the app hydrates
 * Must run BEFORE any components/middleware access auth state
 */
export default defineNuxtPlugin({
    name: 'auth-restore',
    enforce: 'pre', // Run before other plugins
    setup() {
        const { restoreFromStorage } = useAuthState()

        // Restore auth state from localStorage when the app loads on client
        const restored = restoreFromStorage()
        console.log('[Auth Plugin] Restored from storage:', restored)

        // Also check localStorage directly for debugging
        if (import.meta.client) {
            const sessionId = localStorage.getItem('sessionId')
            const user = localStorage.getItem('user')
            console.log('[Auth Plugin] localStorage sessionId:', sessionId ? 'exists' : 'null')
            console.log('[Auth Plugin] localStorage user:', user ? 'exists' : 'null')
        }
    }
})
