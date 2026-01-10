<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const reqId = route.params.id as string

const step = ref(1) // 1: Review, 2: Signing
const loading = ref(false)
const signed = ref(false)

const requestData = {
  id: reqId,
  requester: 'nguyenvana',
  type: 'RA_REQUEST',
  details: {
    fullName: 'Nguyen Van A',
    idCard: '001088012345',
    email: 'nguyenvana@gov.vn',
    algorithm: 'ML-DSA-65',
    reason: 'New Employee Registration'
  }
}

async function startSigning() {
  step.value = 2 // Move to signing UI
}

async function confirmSignAndApprove() {
  loading.value = true
  
  // Mock Officer Signing (Approving with Key)
  setTimeout(() => {
    loading.value = false
    signed.value = true
    setTimeout(() => router.push('/officer'), 2000)
  }, 2000)
}
</script>

<template>
  <div class="page-container">
    <div class="panel">
      <!-- Header -->
      <div class="header">
        <button @click="router.back()" class="btn-back">← Quay lại</button>
        <h2>Phê duyệt Yêu cầu: {{ reqId }}</h2>
      </div>

      <!-- Step 1: Review -->
      <div v-if="step === 1" class="step-content">
        <div class="info-section">
          <h3>Thông tin đăng ký (KYC Data)</h3>
          <div class="form-grid">
            <div class="field">
              <label>Họ tên</label>
              <div class="val">{{ requestData.details.fullName }}</div>
            </div>
            <div class="field">
              <label>Số CCCD</label>
              <div class="val">{{ requestData.details.idCard }}</div>
            </div>
            <div class="field">
              <label>Email</label>
              <div class="val">{{ requestData.details.email }}</div>
            </div>
            <div class="field">
              <label>Thuật toán yêu cầu</label>
              <div class="val tag">{{ requestData.details.algorithm }}</div>
            </div>
          </div>
        </div>

        <div class="actions">
          <button class="btn-reject">Từ chối</button>
          <div class="spacer"></div>
          <button @click="startSigning" class="btn-next">Tiến hành Ký duyệt</button>
        </div>
      </div>

      <!-- Step 2: Officer Sign -->
      <div v-else class="step-content signing-ui">
        <h3><span class="icon">✍️</span> Ký số Cán bộ (Officer Approval)</h3>
        <p class="desc">Vui lòng xác thực và ký số để phê duyệt yêu cầu này.</p>

        <div class="sign-form">
          <div class="field">
            <label>Khóa ký (Officer Key)</label>
            <select disabled>
              <option>OFFICER-KEY-01 (ML-DSA-65)</option>
            </select>
          </div>
          
          <div class="field">
             <label>Mã PIN bảo mật</label>
             <input type="password" value="123456" readonly class="input-pin" />
          </div>

          <div v-if="signed" class="success-box">
             <h4>✓ Đã ký duyệt thành công!</h4>
             <p>Chữ ký: <code class="sig-block">-----BEGIN ML-DSA-65 SIGNATURE-----
MIIFvzCCA6egAwIBAgIUM/WpHYxjDkX/xYrw0+hC7LUjfdswDQYJKoZIhvcNAQELBQAwbzELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURpbmgxDDAKBgNVBAoMA0dvdjEPMA0GA1UECwwGUFFDLUNBMSAwHgYDVQQDDBdNb2NrIFBRQyBTaWduYXR1cmUgRGF0YTAeFw0yNTEyMjkwMTQ5MzlaFw0yNjEyMjkwMTQ5MzlaMG8xCzAJBgNVBAYTAlZOMQ4wDAYDVQQIDAVIYW5vaTEPMA0GA1UEBwwGQmFEaW5oMQwwCgYDVQQKDANHb3YxDzANBgNVBAsMBlBRQy1DQTEgMB4GA1UEAwwXTW9jayBQUUMgU2lnbmF0dXJlIERhdGEwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCbQvIznFp0G0ymvQpwGP3V2oCmR3q+XU3fcLWHx3nJJqQ5lT4niv1/psVBn5dEwphFuNfW6BcxULvbLoNLQ6IK/6qf86SaySbYiZeIFtF6aBekPONKsTWRU5zBkQNSrrL1h03agwwaVFmBxgJ9mDlmZ54t1S7LVXt0c/HZ3oWbrjP15DmK24HB/33dCzVt+Gmbo+u4qtaocHvcDTxpqRnKOl9rDNphcS9SK/u6C1/qW8KYG2jvl0aMuyLHKRuoALkYcLrXlxoA+MDxl0dq2gMPimy1vKltoTIpjhxNsV+0+oJxlVab24Yyuv+NA3tL9LVTglxuk6vUbLBGMZiPL4KODp+QqosUWSeJ5zcaztnyJ4Yw6s1WEgKuhBjzPYLsJ0ffSsT1rzrdivBcAWfBRcuTNQ7fZOxIbpeQqBpEi9S1k6ypdb31CgCdeWhH6BdjsWsTlAynAIqpqwFMLUljdJr5S4nsc4yZewhk1Lcz3fMkX3UESQFy1jW7Uxg1M2bRoHHGehH2z7KDcV6fJrL4LBg6A2W7vdYocCVmAqmbo43HbXvwVRG6IkK+AG7hkosPZLbP81fXBlGzMECVYO2atu4FmvPxzmAhfyEzEoH7svO3Wy31711Tb4Xl4SLlqVYUtDIjT7jOssvgxA50y4ABNO4QnWhNRniw3m8hpaVGMRzOxwIDAQABo1MwUTAdBgNVHQ4EFgQU+6WgqjJHs0UOCYotQ2QMXPz8AiswHwYDVR0jBBgwFoAU+6WgqjJHs0UOCYotQ2QMXPz8AiswDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAgEAhDwo0tlNFWo8qWKarAabwq6soklyS6rOYVbfSjZiGC2qQobBb9xi5rbvu7XCxxXo9jmm3Bg7YsDCRFiN6uuCdCPq3mn0wvLWIhsPIJ5mp4KSQCSzaK6do/jDzn4V5bSi5FRxw5gcD2gZNmRvhZX1xs8mF4g76T15R2aP75gMZnoLv2b+oKPSXkubR6iCm3p2FSY7W7kVEC+/oE/KkbFxtuhehtuBqd++nnqb4IhAxLKJw/5myeqGV9u4M0TZ7J/4nsuuKmm38/UFYfjrCNOWzVUDHG9szv/gddTKf6rMY8wrF5FSSPBO3TMfZxub7s5J7/2/d3RESRe36+lI8DasPONgswz7rfr9DvHBgNfpByFN2WQ1twbGEI2UhkUZccKh45kDm2arQCWVADw7Spxi2W+vwXplyfuhdJA5uZao+Yb02FcJm0+4f9I5bdMbxRfMttSrq5CUaByIoy7Sk7SuxTu2SKavgNYk2uEjyCUgsAm4RqcNGG6WDHa6CJp10J/4lvkiD9Ohlhd/pR/m3Epw61UYYVgO5utS8iNP8xhlw8piwesCO2HZT/k/Qz5dNtg9iLgr+W1ozOcMxESacppC+utpQkosctOocxhd81Mraiul5sTt3m79eHDVzKZr/eAGonxDqwQbv4wob69nOwMNZ7DMZWYAAZniY9/d1rJbl94=
-----END ML-DSA-65 SIGNATURE-----</code> (Verified ML-DSA-65)</p>
             <p>Đang chuyển hướng...</p>
          </div>

          <button v-else @click="confirmSignAndApprove" :disabled="loading" class="btn-sign">
            {{ loading ? 'Đang thực hiện ký (Signing)...' : 'Ký duyệt & Hoàn tất' }}
          </button>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.page-container { display: flex; justify-content: center; padding: 2rem; background: #f0f2f5; min-height: 90vh; }
.panel { background: white; padding: 0; width: 100%; max-width: 800px; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); overflow: hidden; }

.header { background: #1a4d8c; padding: 1.5rem; color: white; display: flex; align-items: center; }
.header h2 { margin: 0; font-size: 1.25rem; }
.btn-back { background: rgba(255,255,255,0.2); border: none; color: white; padding: 0.4rem 0.8rem; margin-right: 1rem; border-radius: 4px; cursor: pointer; }
.btn-back:hover { background: rgba(255,255,255,0.3); }

.step-content { padding: 2rem; }

.info-section { background: #f8f9fa; padding: 1.5rem; border-radius: 6px; margin-bottom: 2rem; }
.info-section h3 { margin-top: 0; color: #555; border-bottom: 1px solid #ddd; padding-bottom: 0.5rem; margin-bottom: 1rem; font-size: 1rem; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.field label { display: block; font-size: 0.85rem; color: #666; margin-bottom: 0.3rem; }
.val { font-weight: 500; color: #333; font-size: 1rem; }
.val.tag { display: inline-block; background: #e3f2fd; color: #0d47a1; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.9rem; }

.actions { display: flex; margin-top: 2rem; border-top: 1px solid #eee; padding-top: 1.5rem; }
.spacer { flex: 1; }
.btn-reject { color: #dc3545; border: 1px solid #dc3545; background: white; padding: 0.8rem 1.5rem; border-radius: 4px; cursor: pointer; font-weight: 500; }
.btn-next { background: #1a4d8c; color: white; border: none; padding: 0.8rem 1.5rem; border-radius: 4px; cursor: pointer; font-weight: 600; font-size: 1rem; }
.btn-next:hover { background: #153d6e; }

.signing-ui { text-align: center; max-width: 500px; margin: 0 auto; }
.signing-ui h3 { color: #2c3e50; font-size: 1.5rem; margin-bottom: 0.5rem; }
.desc { color: #666; margin-bottom: 2rem; }
.sign-form { text-align: left; background: #fafafa; padding: 2rem; border-radius: 8px; border: 1px solid #eee; }
.sign-form .field { margin-bottom: 1.2rem; }
.sign-form select, .sign-form input { width: 100%; padding: 0.8rem; border: 1px solid #ddd; border-radius: 4px; background: white; }
.input-pin { font-family: monospace; letter-spacing: 0.5em; text-align: center; }

.btn-sign { width: 100%; padding: 1rem; background: #28a745; color: white; border: none; border-radius: 4px; font-size: 1.1rem; font-weight: bold; cursor: pointer; margin-top: 1rem; }
.btn-sign:hover { background: #218838; }
.btn-sign:disabled { background: #ccc; cursor: not-allowed; }

.success-box { background: #d4edda; color: #155724; padding: 1.5rem; text-align: center; border-radius: 4px; margin-top: 1rem; border: 1px solid #c3e6cb; }
.success-box code { display: block; margin: 0.5rem 0; word-break: break-all; font-size: 0.75rem; max-height: 200px; overflow-y: auto; text-align: left; background: #fff; padding: 0.5rem; border-radius: 4px; border: 1px solid #c3e6cb; }
</style>
