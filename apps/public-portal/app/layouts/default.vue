<template>
  <div class="app-layout">
    <AppHeader />

    <div class="main-wrapper">
      <main class="main-content">
        <slot />
      </main>
    </div>

    <footer class="footer">
      <p>© 2025 Lê Việt Hoàng - Nguyễn Trọng Nhân</p>
    </footer>
  </div>
</template>

<script setup>
const { checkAuth } = useAuth()

onMounted(() => {
  checkAuth()
})
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.main-wrapper {
  flex: 1;
  display: flex;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
}

.main-content {
  flex: 1;
  padding: 1.5rem;
  width: 100%;
}

.footer {
  background: #333;
  color: #999;
  text-align: center;
  padding: 1rem;
  font-size: 0.8rem;
}
</style>
