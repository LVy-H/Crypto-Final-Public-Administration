// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  srcDir: 'app', // Fix 404: point to app directory

  app: {
    head: {
      title: 'PQC Digital Signature Portal',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Post-Quantum Cryptography Digital Signature System for Public Administration' }
      ],
      link: [
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap' }
      ]
    }
  },

  runtimeConfig: {
    public: {
      // Use relative path for internal API proxy
      apiBase: '/api/v1'
    }
  },

  // Server-side route rules for API proxying
  routeRules: {
    '/api/v1/**': {
      proxy: process.env.NUXT_UPSTREAM_API_URL
        ? `${process.env.NUXT_UPSTREAM_API_URL}/api/v1/**`
        : 'http://api-gateway:8080/api/v1/**'
    },
    // CSC Cloud Signing API proxy
    '/csc/v1/**': {
      proxy: process.env.NUXT_UPSTREAM_API_URL
        ? `${process.env.NUXT_UPSTREAM_API_URL}/csc/v1/**`
        : 'http://api-gateway:8080/csc/v1/**'
    }
  },

  css: ['~/assets/css/main.css']
})
