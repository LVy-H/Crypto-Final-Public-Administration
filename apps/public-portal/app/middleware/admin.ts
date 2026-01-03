/**
 * Admin middleware
 * Requires admin role to access protected routes
 */
export default defineNuxtRouteMiddleware(() => {
    const { loggedIn, user } = useUserSession()

    if (!loggedIn.value) {
        return navigateTo('/login')
    }

    // Check for admin role
    if (user.value?.role !== 'ADMIN' && user.value?.role !== 'OFFICER') {
        return navigateTo('/dashboard')
    }
})
