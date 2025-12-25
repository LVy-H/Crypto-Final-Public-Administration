<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1>🔐 Admin Portal</h1>
        <p>PQC PKI Management</p>
      </div>
      
      <nav class="sidebar-nav">
        <a href="/dashboard" class="nav-item active">
          <span class="icon">📊</span>
          Dashboard
        </a>
        <a href="/ca-management" class="nav-item">
          <span class="icon">🏛️</span>
          CA Management
        </a>
        <a href="/certificates" class="nav-item">
          <span class="icon">📜</span>
          Certificates
        </a>
        <a href="/users" class="nav-item">
          <span class="icon">👥</span>
          Users
        </a>
        <a href="/audit" class="nav-item">
          <span class="icon">📋</span>
          Audit Logs
        </a>
      </nav>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <header class="content-header">
        <h2>PKI Dashboard</h2>
        <div class="header-actions">
          <span class="status-badge">System Active</span>
        </div>
      </header>

      <!-- Stats Grid -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">🏛️</div>
          <div class="stat-info">
            <h3>Root CAs</h3>
            <p class="stat-value">1</p>
            <span class="stat-label">ML-DSA-87</span>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">🏢</div>
          <div class="stat-info">
            <h3>Provincial CAs</h3>
            <p class="stat-value">1</p>
            <span class="stat-label">Active</span>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">🏪</div>
          <div class="stat-info">
            <h3>District RAs</h3>
            <p class="stat-value">1</p>
            <span class="stat-label">ML-DSA-65</span>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">🔒</div>
          <div class="stat-info">
            <h3>Internal CA</h3>
            <p class="stat-value">Active</p>
            <span class="stat-label">mTLS Enabled</span>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <section class="quick-actions">
        <h3>Quick Actions</h3>
        <div class="action-buttons">
          <button class="action-btn" @click="initRootCa">Initialize Root CA</button>
          <button class="action-btn" @click="createProvincialCa">Create Provincial CA</button>
          <button class="action-btn" @click="createInternalCa">Setup Internal mTLS</button>
          <button class="action-btn danger" @click="viewRevocations">View Revocations</button>
        </div>
      </section>

      <!-- System Info -->
      <section class="system-info">
        <h3>System Information</h3>
        <div class="info-grid">
          <div class="info-item">
            <label>PKI Algorithm</label>
            <value>ML-DSA-87 / ML-DSA-65</value>
          </div>
          <div class="info-item">
            <label>TLS Version</label>
            <value>TLS 1.3 (Hybrid PQC)</value>
          </div>
          <div class="info-item">
            <label>Key Exchange</label>
            <value>X25519 + ML-KEM-768</value>
          </div>
          <div class="info-item">
            <label>Security Level</label>
            <value>NIST Level 3-5 (Quantum-Safe)</value>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
const apiBase = useRuntimeConfig().public.apiBase || 'http://localhost:8080/api/v1'

const initRootCa = async () => {
  alert('Initializing Root CA...')
}

const createProvincialCa = async () => {
  alert('Creating Provincial CA...')
}

const createInternalCa = async () => {
  alert('Setting up Internal mTLS CA...')
}

const viewRevocations = () => {
  alert('Viewing Revocation List...')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #0f0f23;
  color: #e0e0e0;
}

.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1a1a3e 0%, #0f0f23 100%);
  border-right: 1px solid #333366;
  padding: 2rem 0;
}

.sidebar-header {
  padding: 0 1.5rem 2rem;
  border-bottom: 1px solid #333366;
}

.sidebar-header h1 {
  font-size: 1.4rem;
  margin: 0;
  color: #7c3aed;
}

.sidebar-header p {
  margin: 0.5rem 0 0;
  font-size: 0.85rem;
  color: #888;
}

.sidebar-nav {
  padding: 1.5rem 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1.5rem;
  color: #999;
  text-decoration: none;
  transition: all 0.2s;
}

.nav-item:hover, .nav-item.active {
  background: rgba(124, 58, 237, 0.1);
  color: #7c3aed;
  border-left: 3px solid #7c3aed;
}

.icon {
  font-size: 1.2rem;
}

.main-content {
  flex: 1;
  padding: 2rem;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.content-header h2 {
  margin: 0;
  font-size: 1.75rem;
}

.status-badge {
  background: linear-gradient(135deg, #10b981, #059669);
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.85rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: linear-gradient(145deg, #1a1a3e, #0f0f23);
  border: 1px solid #333366;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  font-size: 2.5rem;
}

.stat-info h3 {
  margin: 0;
  font-size: 0.9rem;
  color: #888;
}

.stat-value {
  margin: 0.25rem 0;
  font-size: 1.75rem;
  font-weight: 600;
  color: #7c3aed;
}

.stat-label {
  font-size: 0.8rem;
  color: #666;
}

.quick-actions, .system-info {
  background: rgba(26, 26, 62, 0.5);
  border: 1px solid #333366;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.quick-actions h3, .system-info h3 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.action-btn {
  background: linear-gradient(135deg, #7c3aed, #5b21b6);
  border: none;
  color: white;
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(124, 58, 237, 0.3);
}

.action-btn.danger {
  background: linear-gradient(135deg, #dc2626, #991b1b);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.info-item {
  background: rgba(0, 0, 0, 0.2);
  padding: 1rem;
  border-radius: 8px;
}

.info-item label {
  display: block;
  font-size: 0.8rem;
  color: #888;
  margin-bottom: 0.25rem;
}

.info-item value {
  font-size: 1rem;
  color: #10b981;
}
</style>
