<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header>
    <div class="wrapper">
      <nav>
        <RouterLink to="/register">Register</RouterLink> |
        <RouterLink to="/dashboard">Dashboard</RouterLink> |
        <RouterLink to="/sign">Sign</RouterLink> |
        <RouterLink to="/verify">Verify</RouterLink> |
        <RouterLink to="/officer" class="admin-link">Officer Panel</RouterLink>
      </nav>
    </div>
  </header>

  <RouterView />
</template>

<style scoped>
header {
  line-height: 1.5;
  max-height: 100vh;
  background-color: #f8f9fa;
  border-bottom: 1px solid #ddd;
  padding: 1rem;
}

nav {
  width: 100%;
  font-size: 1rem;
  text-align: center;
}

nav a {
  display: inline-block;
  padding: 0 1rem;
  border-left: 1px solid var(--color-border);
  text-decoration: none;
  color: #1a4d8c; 
  font-weight: 500;
}

nav a:first-of-type {
  border: 0;
}

nav a.router-link-exact-active {
  color: #1a4d8c;
  font-weight: 700;
  text-decoration: underline;
}
</style>
