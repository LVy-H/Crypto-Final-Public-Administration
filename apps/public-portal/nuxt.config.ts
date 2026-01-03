// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  // Fix 404: point to app directory
  srcDir: 'app',

  modules: [
    '@nuxt/ui',           // Nuxt UI 4 (includes Tailwind)
    'nuxt-auth-utils',    // Session management
    '@vueuse/nuxt'        // VueUse composables
  ],

  // Nuxt UI theming
  ui: {
    colors: {
      primary: 'blue',
      neutral: 'slate'
    }
  },

  app: {
    head: {
      title: 'PQC Digital Signature Portal',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Post-Quantum Cryptography Digital Signature System for Public Administration' }
      ]
    }
  },

  runtimeConfig: {
    public: {
      apiBase: '/api/v1'
    }
  },

  // Server-side route rules for API proxying
  // Note: /api/v1/auth/** is handled by explicit server routes in /server/api/
  routeRules: {
    '/csc/v1/**': {
      proxy: process.env.NUXT_UPSTREAM_API_URL
        ? `${process.env.NUXT_UPSTREAM_API_URL}/csc/v1/**`
        : 'http://api-gateway:8080/csc/v1/**'
    }
  }
})