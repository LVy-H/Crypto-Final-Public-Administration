/**
 * Auth middleware that protects routes requiring authentication
 * Uses the new useAuthState composable for SSR-safe auth checking
 */
export default defineNuxtRouteMiddleware((to) => {
  const { isAuthenticated, hasAdminAccess, restoreFromStorage } = useAuthState()

  // Public routes that don't require auth
  const publicRoutes = ['/', '/login', '/register']
  if (publicRoutes.includes(to.path)) {
    return
  }

  // On client, restore from storage if not authenticated
  if (import.meta.client && !isAuthenticated.value) {
    restoreFromStorage()
  }

  // Require authentication for all other routes  
  if (!isAuthenticated.value) {
    return navigateTo('/login')
  }

  // Admin routes require admin/officer access
  if (to.path.startsWith('/admin') && !hasAdminAccess.value) {
    return navigateTo('/dashboard')
  }
})
