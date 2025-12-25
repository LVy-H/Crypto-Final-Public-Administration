<template>
  <div class="home-page">
    <nav class="navbar glass">
      <div class="container flex items-center justify-between">
        <div class="nav-brand">
          <span class="brand-icon">🔐</span>
          <span class="brand-text">PQC Digital Signature</span>
        </div>
        <div class="nav-links">
          <NuxtLink to="/login" class="btn btn-secondary">Sign In</NuxtLink>
          <NuxtLink to="/register" class="btn btn-primary">Get Started</NuxtLink>
        </div>
      </div>
    </nav>

    <main>
      <!-- Hero Section -->
      <section class="hero">
        <div class="container">
          <div class="hero-content fade-in">
            <div class="hero-badge">🛡️ Post-Quantum Secure</div>
            <h1 class="hero-title">
              Digital Signatures for the 
              <span class="gradient-text">Quantum Era</span>
            </h1>
            <p class="hero-subtitle">
              Secure your documents with ML-DSA (Dilithium) — the NIST-standardized 
              post-quantum digital signature algorithm. Built for Vietnamese public administration.
            </p>
            <div class="hero-actions">
              <NuxtLink to="/register" class="btn btn-primary btn-lg">Start Signing →</NuxtLink>
              <a href="#features" class="btn btn-secondary btn-lg">Learn More</a>
            </div>
          </div>
          <div class="hero-visual fade-in">
            <div class="visual-card">
              <div class="visual-header">
                <span class="visual-dot"></span>
                <span class="visual-dot"></span>
                <span class="visual-dot"></span>
              </div>
              <div class="visual-content">
                <div class="visual-icon">📄</div>
                <div class="visual-sig">
                  <span>✍️ Signed with ML-DSA-44</span>
                  <span class="badge badge-success">Verified</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section id="features" class="features">
        <div class="container">
          <h2 class="section-title text-center">Why Choose PQC Signatures?</h2>
          <div class="grid grid-3">
            <div class="feature-card card fade-in">
              <div class="feature-icon">🔒</div>
              <h3>Quantum-Resistant</h3>
              <p class="text-muted">Protected against both classical and quantum computer attacks using NIST-approved algorithms.</p>
            </div>
            <div class="feature-card card fade-in">
              <div class="feature-icon">📜</div>
              <h3>Legally Valid</h3>
              <p class="text-muted">Compliant with Vietnamese Decree 130/2018/NĐ-CP and international eIDAS standards.</p>
            </div>
            <div class="feature-card card fade-in">
              <div class="feature-icon">⚡</div>
              <h3>Fast & Easy</h3>
              <p class="text-muted">Sign documents in seconds with our cloud-based HSM infrastructure.</p>
            </div>
          </div>
        </div>
      </section>

      <!-- CTA Section -->
      <section class="cta">
        <div class="container text-center">
          <h2>Ready to Get Started?</h2>
          <p class="text-muted">Register now to receive your quantum-resistant digital certificate</p>
          <NuxtLink to="/register" class="btn btn-primary btn-lg mt-8">Create Account</NuxtLink>
        </div>
      </section>
    </main>

    <footer class="footer">
      <div class="container text-center">
        <p class="text-muted">© 2024 PQC Digital Signature System • Government of Vietnam</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
}

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 1rem 0;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.brand-icon {
  font-size: 1.5rem;
}

.brand-text {
  font-weight: 700;
  font-size: 1.125rem;
}

.nav-links {
  display: flex;
  gap: 0.75rem;
}

.hero {
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding-top: 5rem;
  background: 
    radial-gradient(circle at 20% 30%, rgba(99, 102, 241, 0.2), transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(14, 165, 233, 0.15), transparent 40%),
    var(--bg-dark);
}

.hero .container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: center;
}

.hero-badge {
  display: inline-block;
  background: rgba(99, 102, 241, 0.2);
  color: var(--primary);
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 1.5rem;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 1.5rem;
}

.gradient-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 1.25rem;
  color: var(--text-secondary);
  margin-bottom: 2rem;
  max-width: 500px;
}

.hero-actions {
  display: flex;
  gap: 1rem;
}

.btn-lg {
  padding: 1rem 2rem;
  font-size: 1rem;
}

.hero-visual {
  display: flex;
  justify-content: center;
}

.visual-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  width: 350px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.visual-header {
  background: var(--bg-dark);
  padding: 0.75rem 1rem;
  display: flex;
  gap: 0.5rem;
}

.visual-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--border);
}

.visual-dot:first-child { background: #ef4444; }
.visual-dot:nth-child(2) { background: #f59e0b; }
.visual-dot:nth-child(3) { background: #10b981; }

.visual-content {
  padding: 2rem;
  text-align: center;
}

.visual-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.visual-sig {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: var(--bg-dark);
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
}

.features {
  padding: 6rem 0;
}

.section-title {
  font-size: 2rem;
  margin-bottom: 3rem;
}

.feature-card {
  text-align: center;
  padding: 2rem;
}

.feature-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.feature-card h3 {
  margin-bottom: 0.5rem;
}

.cta {
  padding: 6rem 0;
  background: 
    radial-gradient(circle at 50% 50%, rgba(99, 102, 241, 0.1), transparent 60%),
    var(--bg-card);
}

.cta h2 {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.footer {
  padding: 2rem 0;
  border-top: 1px solid var(--border);
}

@media (max-width: 768px) {
  .hero .container {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .hero-title {
    font-size: 2.5rem;
  }
  
  .hero-subtitle {
    margin-left: auto;
    margin-right: auto;
  }
  
  .hero-actions {
    justify-content: center;
  }
  
  .hero-visual {
    display: none;
  }
}
</style>
