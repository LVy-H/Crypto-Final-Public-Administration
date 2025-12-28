export default defineNuxtRouteMiddleware((to) => {
    const { checkAuth, hasAdminAccess } = useAuth()

    // Check authentication
    const isAuth = checkAuth()

    // Public routes that don't require auth
    const publicRoutes = ['/', '/login', '/register']
    if (publicRoutes.includes(to.path)) {
        return
    }

    // Require authentication for all other routes
    if (!isAuth) {
        return navigateTo('/login')
    }

    // Admin routes require admin/officer access
    if (to.path.startsWith('/admin') && !hasAdminAccess.value) {
        return navigateTo('/dashboard')
    }
})
