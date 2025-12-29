<script setup lang="ts">
import { ref } from 'vue'

const keys = ['key_mldsa65_alias', 'key_mldsa44_alias']
const selectedKey = ref(keys[0])
const file = ref<File | null>(null)
const signature = ref('')
const loading = ref(false)

async function handleSign() {
  if (!file.value) return 
  loading.value = true

  // 1. Client-side hashing (Mocked)
  const hash = 'dGVzdCBoYXNo' // 'test hash' in base64
  
  // 2. CSC Sign Call
  // POST /csc/v1/sign
  setTimeout(() => {
    // Mock response with realistic OpenSSL SHA256 signature
    // Mock response with realistic OpenSSL ML-DSA-65 signature (PQC) - Fresh Generation v4
    signature.value = `-----BEGIN ML-DSA-65 SIGNATURE-----
MIIFvzCCA6egAwIBAgIUM/WpHYxjDkX/xYrw0+hC7LUjfdswDQYJKoZIhvcNAQELBQAwbzELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURpbmgxDDAKBgNVBAoMA0dvdjEPMA0GA1UECwwGUFFDLUNBMSAwHgYDVQQDDBdNb2NrIFBRQyBTaWduYXR1cmUgRGF0YTAeFw0yNTEyMjkwMTQ5MzlaFw0yNjEyMjkwMTQ5MzlaMG8xCzAJBgNVBAYTAlZOMQ4wDAYDVQQIDAVIYW5vaTEPMA0GA1UEBwwGQmFEaW5oMQwwCgYDVQQKDANHb3YxDzANBgNVBAsMBlBRQy1DQTEgMB4GA1UEAwwXTW9jayBQUUMgU2lnbmF0dXJlIERhdGEwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCbQvIznFp0G0ymvQpwGP3V2oCmR3q+XU3fcLWHx3nJJqQ5lT4niv1/psVBn5dEwphFuNfW6BcxULvbLoNLQ6IK/6qf86SaySbYiZeIFtF6aBekPONKsTWRU5zBkQNSrrL1h03agwwaVFmBxgJ9mDlmZ54t1S7LVXt0c/HZ3oWbrjP15DmK24HB/33dCzVt+Gmbo+u4qtaocHvcDTxpqRnKOl9rDNphcS9SK/u6C1/qW8KYG2jvl0aMuyLHKRuoALkYcLrXlxoA+MDxl0dq2gMPimy1vKltoTIpjhxNsV+0+oJxlVab24Yyuv+NA3tL9LVTglxuk6vUbLBGMZiPL4KODp+QqosUWSeJ5zcaztnyJ4Yw6s1WEgKuhBjzPYLsJ0ffSsT1rzrdivBcAWfBRcuTNQ7fZOxIbpeQqBpEi9S1k6ypdb31CgCdeWhH6BdjsWsTlAynAIqpqwFMLUljdJr5S4nsc4yZewhk1Lcz3fMkX3UESQFy1jW7Uxg1M2bRoHHGehH2z7KDcV6fJrL4LBg6A2W7vdYocCVmAqmbo43HbXvwVRG6IkK+AG7hkosPZLbP81fXBlGzMECVYO2atu4FmvPxzmAhfyEzEoH7svO3Wy31711Tb4Xl4SLlqVYUtDIjT7jOssvgxA50y4ABNO4QnWhNRniw3m8hpaVGMRzOxwIDAQABo1MwUTAdBgNVHQ4EFgQU+6WgqjJHs0UOCYotQ2QMXPz8AiswHwYDVR0jBBgwFoAU+6WgqjJHs0UOCYotQ2QMXPz8AiswDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAgEAhDwo0tlNFWo8qWKarAabwq6soklyS6rOYVbfSjZiGC2qQobBb9xi5rbvu7XCxxXo9jmm3Bg7YsDCRFiN6uuCdCPq3mn0wvLWIhsPIJ5mp4KSQCSzaK6do/jDzn4V5bSi5FRxw5gcD2gZNmRvhZX1xs8mF4g76T15R2aP75gMZnoLv2b+oKPSXkubR6iCm3p2FSY7W7kVEC+/oE/KkbFxtuhehtuBqd++nnqb4IhAxLKJw/5myeqGV9u4M0TZ7J/4nsuuKmm38/UFYfjrCNOWzVUDHG9szv/gddTKf6rMY8wrF5FSSPBO3TMfZxub7s5J7/2/d3RESRe36+lI8DasPONgswz7rfr9DvHBgNfpByFN2WQ1twbGEI2UhkUZccKh45kDm2arQCWVADw7Spxi2W+vwXplyfuhdJA5uZao+Yb02FcJm0+4f9I5bdMbxRfMttSrq5CUaByIoy7Sk7SuxTu2SKavgNYk2uEjyCUgsAm4RqcNGG6WDHa6CJp10J/4lvkiD9Ohlhd/pR/m3Epw61UYYVgO5utS8iNP8xhlw8piwesCO2HZT/k/Qz5dNtg9iLgr+W1ozOcMxESacppC+utpQkosctOocxhd81Mraiul5sTt3m79eHDVzKZr/eAGonxDqwQbv4wob69nOwMNZ7DMZWYAAZniY9/d1rJbl94=
-----END ML-DSA-65 SIGNATURE-----`
    loading.value = false
  }, 1500)
}

function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) file.value = target.files[0]
}
</script>

<template>
  <div class="page-container">
    <div class="panel">
      <h2>Ký số từ xa (Cloud Signing - CSC)</h2>
      
      <div class="form-group">
        <label>Chọn khóa (Key Alias)</label>
        <select v-model="selectedKey">
          <option v-for="k in keys" :key="k" :value="k">{{ k }}</option>
        </select>
      </div>

      <div class="form-group">
        <label>Tải lên tài liệu (PDF/XML)</label>
        <div class="upload-box">
          <input type="file" @change="onFileChange" />
          <p v-if="file">Đã chọn: {{ file.name }}</p>
          <p v-else>Kéo thả hoặc nhấn để chọn</p>
        </div>
      </div>

      <button @click="handleSign" :disabled="!file || loading" class="btn-sign">
        {{ loading ? 'Đang ký (Signing)...' : 'Ký ngay' }}
      </button>

      <div v-if="signature" class="result">
        <label>Chữ ký tạo được (Base64):</label>
        <textarea readonly :value="signature"></textarea>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { display: flex; justify-content: center; padding: 2rem; }
.panel { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 100%; max-width: 500px; }
h2 { color: #1a4d8c; margin-bottom: 1.5rem; text-align: center; }
.form-group { margin-bottom: 1.5rem; }
.form-group label { display: block; font-weight: 500; margin-bottom: 0.5rem; }
select { width: 100%; pading: 0.5rem; height: 40px; }
.upload-box { border: 2px dashed #ccc; padding: 1.5rem; text-align: center; background: #fafafa; position: relative; }
.upload-box input { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }
.btn-sign { width: 100%; background: #28a745; color: white; padding: 1rem; border: none; border-radius: 4px; font-size: 1.1rem; cursor: pointer; font-weight: bold; }
.btn-sign:disabled { background: #ccc; }
.result { margin-top: 2rem; background: #f8f9fa; padding: 1rem; border: 1px solid #ddd; border-radius: 4px; }
.result textarea { width: 100%; height: 80px; margin-top: 0.5rem; padding: 0.5rem; font-family: monospace; }
</style>
