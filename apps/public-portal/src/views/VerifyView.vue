<script setup lang="ts">
import { ref } from 'vue'

const file = ref<File | null>(null)
const signature = ref('')
const loading = ref(false)
const verificationResult = ref<any>(null)
const showChain = ref(true) // Default true for screenshot visibility

async function handleVerify() {
  loading.value = true
  verificationResult.value = null
  
  // Mock POST /api/v1/validation/verify
  setTimeout(() => {
    loading.value = false
    const rand = Math.random()
    if (rand > 0.1) { // 90% success for demo
      verificationResult.value = {
        isValid: true,
        verifiedAt: new Date().toLocaleString('vi-VN'),
        details: {
          hash: 'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2', // Placeholder hash
          chainStatus: 'Verified (Root CA: VN-GOV-CA-L1)',
          tsa: {
            show: true,
            subject: 'CN=VN-GOV-TSA-TimeStamping, OU=National-PQC-TSA, O=Gov, C=VN',
            timestamp: '2025-12-29T02:02:49.123Z',
            value: `-----BEGIN CERTIFICATE-----
MIIFwDCCA6igAwIBAgIULq1Nla298NduwkLVNAbFD6ve4sIwDQYJKoZIhvcNAQEL
BQAwbzELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURp
bmgxDDAKBgNVBAoMA0dvdjEPMA0GA1UECwwGUFFDLUNBMSAwHgYDVQQDDBdNb2Nr
IFBRQyBTaWduYXR1cmUgRGF0YTAeFw0yNTEyMjkwMTQ5MzlaFw0yNjEyMjkwMTQ5
MzlaMGcxCzAJBgNVBAYTAlZOMQ4wDAYDVQQIDAVIYW5vaTEPMA0GA1UEBwwGQmFE
aW5oMQwwCgYDVQQKDANHb3YxGTAXBgNVBAsMLE5hdGlvbmFsLVBRQy1UU0ExGTAX
BgNVBAMMLEZOEdOVjhUU0EtVGltZVN0YW1waW5nMIIBIjANBgkqhkiG9w0BAQEF
AAOCAQ8AMIIBCgKCAQEAst4fKK5bb7AyWBlhNiAsDRlMq2QDRrky9keupjTZQprY
4ELsMJrV8lvp9P5cHv3+oHB6fPxoc3Th3/v2C2Q2CX+JTa5QnIbyJsZzY3qS3nTR
19Dx9so/l+0mb0r9s77m4NrliEBqdtPN20YU+9246+je5mSRK6tV8t6y+f8no/DW
e/DJ56ivZbcDa2UxGjFkVtqIITpGWFOYW/tP1ojN3OD7UbHWgEbZJrvIf+00B+kt
giF3ThFCvkRUy1MnkwHLImoGlt0wMq5n6cN+Vh0KATD8Jhy9z6Ce6GNurZ7gZlrm
XX9e+ILAB4W8Va95wskAbtlg3bvn6T+Jd7D2n7UCAwEAAaNTMFEwHQYDVR0OBBYE
FFgfME2iAm2sqAhCz2vVCZXhFNmHMB8GA1UdIwQYMBaAFFgfME2iAm2sqAhCz2vV
CZXhFNmHMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBACNJCUXC
PwHPP83Wtn5z35fD7DngX+39mFXKIdsI2RE+9JDZTBiFVIqis996z7iNH9EajPdg
+7mZY+0YIAfY+EnZDImq+OwFNrpMDoYIntn7xqtuWRN5sO/VbXxpskulpSnnzBqM
0FnCmFteHsub8FwAQERJNjPU+vTamjOq02Q+KlJZ1fyMFJ1Tivb9d20D3xXWXYIV
R9qGZdOobwF4wV2dQV8PubUYxRxohdbwwB2s1W4tAuKdZ8+ldvCV9rPTS4UbH/Oi
Frl59skCp6tJeweUiwpJ0PTfaMEGXKkAOEGiQ8NmNdCLPpl96jbt73e0tZ1eAG6F
JNeZyg361SXaNzk=
-----END CERTIFICATE-----`
          },
          trustChain: [
            {
              subject: 'CN=VN-GOV-Root-CA, OU=National-PQC-Root-CA, O=Gov, C=VN',
              issuer: 'CN=VN-GOV-Root-CA, OU=National-PQC-Root-CA, O=Gov, C=VN',
              validity: '2025-2045',
              fingerprint: 'A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0',
              value: `-----BEGIN CERTIFICATE-----
MIIFyTCCA7GgAwIBAgIULqxNla298NduwkLVNAbFD6ve4sIwDQYJKoZIhvcNAQEL
BQAwdDELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURp
bmgxDDAKBgNVBAoMA0dvdjEdMBsGA1UECwwUTmF0aW9uYWwtUFFDLVJvb3QtQ0Ex
FzAVBgNVBAMMDlZOLUdPVi1Sb290LUNBMB4XDTI1MTIyOTAyMDAyOFoXDTI2MTIy
OTAyMDAyOFowdDELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQH
DAZCYURpbmgxDDAKBgNVBAoMA0dvdjEdMBsGA1UECwwUTmF0aW9uYWwtUFFDLVJv
b3QtQ0ExFzAVBgNVBAMMDlZOLUdPVi1Sb290LUNBMIICIjANBgkqhkiG9w0BAQEF
AAOCAg8AMIICCgKCAgEA2k8o6PbggJ2kvvJxgQvKDDMiI8yAN+aqgB8enL1JdcFA
TOtYtnN4sMLLYDEp6LTcNNS5TAllOmh1OH6safFk8evNQZlZ82Oj5ttCy3plLUQv
hUwa1/VCJkHSCjtopsUiHxnszyVvhNjiyyKDdaHDIw/cGPOBa4W06rtCvRicxcrA
//FJHb8UsU4UBtmZGuBLGExHGpryWQilyf5ADfJ4SNVHIhr1l5gVT5LlFF6eDjqq
1f4Ufv0NBvXsaF09BzhwyKZPjwdjBayZL6Vgmf5T2uJ9677svxdO86p5O+NMH+00
NeJwZfcc0oRhe0N7C/HVNnX1HTR8AbttXqXt/52u0oxhR4KteghTO8kPJ74Vkdjq
NtamvppoQoq8TwfH4yRmcFCVIfmQ6gFcl6ITbodpevLail4muOmzrepWuSkD9g8K
93j2plSKp0HQhEEgOV2yEgTqf1AjcX3fsClBP2Jh24eiSG8rf8HPhDFpbzfjeB4e
XC7iHu9HzAQ6fVq1LQ9pS1H/pJBKCpWSThqtaHC+6sIGS3ixp1u6sfudBchuvKjo
UvWQVqc42Tl79kInmPgYuaZ/eFdruhwPJoGEXI/biUDCkUeAUVCfnfJxGxlo4GoA
78LSDQieQwFlIfKhKS1P3zm5y+Ql2CGke8wFnBTT7PYbGz5pb6SfC2tQVrld/bkC
AwEAAaNTMFEwHQYDVR0OBBYEFEnhyRmv8PvYXJ231YLINSqfQz03MB8GA1UdIwQY
MBaAFEnhyRmv8PvYXJ231YLINSqfQz03MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZI
hvcNAQELBQADggIBAHVXPi3xOOc8dAEZQDY97YibXKGaMqNcijGCsYCVoGrgGCTL
02MYFyiWCvOy9pTYU4bAYefe2seFBtOF97yDAafySIRxFRGlMvYV3gj7ZBQUJbE2
g8ykH5z8KzGVLaag7EfWI/7UUSxTcbiRdo4xU0F9NQEv2bfUOVocZvtrQp/gOlEs
WNMsg31DMjqOMd7SfuciJLMft6ujUPV09u5CgZL3rmEvE6UYroQR9NnICuL5ymJO
DHVzwSLgLxLzvD3AEyez0kLCKJqEw0ftySi4+6pK6bqgeDPuv252AYsFpW4NakkR
09FavwI/l2BOpzJOet1Yfsr/7fYbHd5Q2h8ZsbZM1bqlTmYzuWvkMf5juxjyrqYi
HgifD1xgsO9IJyvd2H+HNJMMbrj/Lf9zbdyUTlN5BSgdliBBmmcEhsUJRMn/9blZ
NoIxBDoedqUWGGB3wg50dDLl0fJVxHOjinB8QKoqFOzIu7cja/gEacALP6Y+2dY0
hfjqDXDYbUZG+3v82ai2n54o3GYEJtkjyh/9xyUlgEeM/xwB1wEfDGM+DdarIdk7
2+kcPQkaONRgPCpiKoCWpnY3waPyIELYMUhr6V4iD3taQF5qn+nE26/XguEg5LEP
RZjHtR3cB3LkVPpqXuAIitmWZwWWu7c12IWetEQYdarL3wG7ON2xVzXZdfXF
-----END CERTIFICATE-----`
            },
            {
              subject: 'CN=VN-GOV-Intermediate-CA, OU=National-PQC-Intermediate-CA, O=Gov, C=VN',
              issuer: 'CN=VN-GOV-Root-CA, OU=National-PQC-Root-CA, O=Gov, C=VN',
              validity: '2025-2035',
              fingerprint: 'B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1',
              value: `-----BEGIN CERTIFICATE-----
MIIFyTCCA7GgAwIBAgIUJ+z4C5y5x5y5x5y5x5y5x5y5x5y5x5y5x5y5x5y5x5y5
BQAwdDELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURp
bmgxDDAKBgNVBAoMA0dvdjEdMBsGA1UECwwUTmF0aW9uYWwtUFFDLVJvb3QtQ0Ex
FzAVBgNVBAMMDlZOLUdPVi1Sb290LUNBMB4XDTI1MTIyOTAyMDAyOFoXDTI2MTIy
OTAyMDAyOFowdDELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQH
DAZCYURpbmgxDDAKBgNVBAoMA0dvdjElMCMGA1UECwwcTmF0aW9uYWwtUFFDLU
luLXRlcm1lZGlhdGUtQ0ExHzAdBgNVBAMMFlZOLUdPVi1JbnRlcm1lZGlhdGUt
Q0BMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA2k8o6PbggJ2kvvJx
gQvKDDMiI8yAN+aqgB8enL1JdcFATOtYtnN4sMLLYDEp6LTcNNS5TAllOmh1OH6s
afFk8evNQZlZ82Oj5ttCy3plLUQvhUwa1/VCJkHSCjtopsUiHxnszyVvhNjiyyKD
daHDIw/cGPOBa4W06rtCvRicxcrA//FJHb8UsU4UBtmZGuBLGExHGpryWQilyf5A
DfJ4SNVHIhr1l5gVT5LlFF6eDjqq1f4Ufv0NBvXsaF09BzhwyKZPjwdjBayZL6Vg
mf5T2uJ9677svxdO86p5O+NMH+00NeJwZfcc0oRhe0N7C/HVNnX1HTR8AbttXqXt
/52u0oxhR4KteghTO8kPJ74VkdjqNtamvppoQoq8TwfH4yRmcFCVIfmQ6gFcl6IT
bodpevLail4muOmzrepWuSkD9g8K93j2plSKp0HQhEEgOV2yEgTqf1AjcX3fsClB
P2Jh24eiSG8rf8HPhDFpbzfjeB4eXC7iHu9HzAQ6fVq1LQ9pS1H/pJBKCpWSThqt
aHC+6sIGS3ixp1u6sfudBchuvKjoUvWQVqc42Tl79kInmPgYuaZ/eFdruhwPJoGE
XI/biUDCkUeAUVCfnfJxGxlo4GoA78LSDQieQwFlIfKhKS1P3zm5y+Ql2CGke8wF
nBTT7PYbGz5pb6SfC2tQVrld/bkCAwEAAaNTMFEwHQYDVR0OBBYEFEnhyRmv8PvY
XJ231YLINSqfQz03MB8GA1UdIwQYMBaAFEnhyRmv8PvYXJ231YLINSqfQz03MA8G
A1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggIBAHVXPi3xOOc8dAEZQDY9
7YibXKGaMqNcijGCsYCVoGrgGCTL02MYFyiWCvOy9pTYU4bAYefe2seFBtOF97y
DAafySIRxFRGlMvYV3gj7ZBQUJbE2g8ykH5z8KzGVLaag7EfWI/7UUSxTcbiRdo4
xU0F9NQEv2bfUOVocZvtrQp/gOlEsWNMsg31DMjqOMd7SfuciJLMft6ujUPV09u5
CgZL3rmEvE6UYroQR9NnICuL5ymJODHVzwSLgLxLzvD3AEyez0kLCKJqEw0ftyS
i4+6pK6bqgeDPuv252AYsFpW4NakkR09FavwI/l2BOpzJOet1Yfsr/7fYbHd5Q2h
8ZsbZM1bqlTmYzuWvkMf5juxjyrqYiHgifD1xgsO9IJyvd2H+HNJMMbrj/Lf9zb
dyUTlN5BSgdliBBmmcEhsUJRMn/9blZNoIxBDoedqUWGGB3wg50dDLl0fJVxHOji
nB8QKoqFOzIu7cja/gEacALP6Y+2dY0hfjqDXDYbUZG+3v82ai2n54o3GYEJtkjy
h/9xyUlgEeM/xwB1wEfDGM+DdarIdk72+kcPQkaONRgPCpiKoCWpnY3waPyIELYM
Uhr6V4iD3taQF5qn+nE26/XguEg5LEPRZjHtR3cB3LkVPpqXuAIitmWZwWWu7c1
2IWetEQYdarL3wG7ON2xVzXZdfXF
-----END CERTIFICATE-----`
            }
          ],
          signatures: [
            {
              signer: 'Nguyen Van A (Citizen)',
              algorithm: 'ML-DSA-65',
              timestamp: new Date().toISOString(),
              value: `-----BEGIN ML-DSA-65 SIGNATURE-----
MIIFvzCCA6egAwIBAgIUM/WpHYxjDkX/xYrw0+hC7LUjfdswDQYJKoZIhvcNAQELBQAwbzELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURpbmgxDDAKBgNVBAoMA0dvdjEPMA0GA1UECwwGUFFDLUNBMSAwHgYDVQQDDBdNb2NrIFBRQyBTaWduYXR1cmUgRGF0YTAeFw0yNTEyMjkwMTQ5MzlaFw0yNjEyMjkwMTQ5MzlaMG8xCzAJBgNVBAYTAlZOMQ4wDAYDVQQIDAVIYW5vaTEPMA0GA1UEBwwGQmFEaW5oMQwwCgYDVQQKDANHb3YxDzANBgNVBAsMBlBRQy1DQTEgMB4GA1UEAwwXTW9jayBQUUMgU2lnbmF0dXJlIERhdGEwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCbQvIznFp0G0ymvQpwGP3V2oCmR3q+XU3fcLWHx3nJJqQ5lT4niv1/psVBn5dEwphFuNfW6BcxULvbLoNLQ6IK/6qf86SaySbYiZeIFtF6aBekPONKsTWRU5zBkQNSrrL1h03agwwaVFmBxgJ9mDlmZ54t1S7LVXt0c/HZ3oWbrjP15DmK24HB/33dCzVt+Gmbo+u4qtaocHvcDTxpqRnKOl9rDNphcS9SK/u6C1/qW8KYG2jvl0aMuyLHKRuoALkYcLrXlxoA+MDxl0dq2gMPimy1vKltoTIpjhxNsV+0+oJxlVab24Yyuv+NA3tL9LVTglxuk6vUbLBGMZiPL4KODp+QqosUWSeJ5zcaztnyJ4Yw6s1WEgKuhBjzPYLsJ0ffSsT1rzrdivBcAWfBRcuTNQ7fZOxIbpeQqBpEi9S1k6ypdb31CgCdeWhH6BdjsWsTlAynAIqpqwFMLUljdJr5S4nsc4yZewhk1Lcz3fMkX3UESQFy1jW7Uxg1M2bRoHHGehH2z7KDcV6fJrL4LBg6A2W7vdYocCVmAqmbo43HbXvwVRG6IkK+AG7hkosPZLbP81fXBlGzMECVYO2atu4FmvPxzmAhfyEzEoH7svO3Wy31711Tb4Xl4SLlqVYUtDIjT7jOssvgxA50y4ABNO4QnWhNRniw3m8hpaVGMRzOxwIDAQABo1MwUTAdBgNVHQ4EFgQU+6WgqjJHs0UOCYotQ2QMXPz8AiswHwYDVR0jBBgwFoAU+6WgqjJHs0UOCYotQ2QMXPz8AiswDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAgEAhDwo0tlNFWo8qWKarAabwq6soklyS6rOYVbfSjZiGC2qQobBb9xi5rbvu7XCxxXo9jmm3Bg7YsDCRFiN6uuCdCPq3mn0wvLWIhsPIJ5mp4KSQCSzaK6do/jDzn4V5bSi5FRxw5gcD2gZNmRvhZX1xs8mF4g76T15R2aP75gMZnoLv2b+oKPSXkubR6iCm3p2FSY7W7kVEC+/oE/KkbFxtuhehtuBqd++nnqb4IhAxLKJw/5myeqGV9u4M0TZ7J/4nsuuKmm38/UFYfjrCNOWzVUDHG9szv/gddTKf6rMY8wrF5FSSPBO3TMfZxub7s5J7/2/d3RESRe36+lI8DasPONgswz7rfr9DvHBgNfpByFN2WQ1twbGEI2UhkUZccKh45kDm2arQCWVADw7Spxi2W+vwXplyfuhdJA5uZao+Yb02FcJm0+4f9I5bdMbxRfMttSrq5CUaByIoy7Sk7SuxTu2SKavgNYk2uEjyCUgsAm4RqcNGG6WDHa6CJp10J/4lvkiD9Ohlhd/pR/m3Epw61UYYVgO5utS8iNP8xhlw8piwesCO2HZT/k/Qz5dNtg9iLgr+W1ozOcMxESacppC+utpQkosctOocxhd81Mraiul5sTt3m79eHDVzKZr/eAGonxDqwQbv4wob69nOwMNZ7DMZWYAAZniY9/d1rJbl94=
-----END ML-DSA-65 SIGNATURE-----`
            },
            {
              signer: 'Officer B (Administration)',
              algorithm: 'ML-DSA-65',
              timestamp: new Date(Date.now() + 3600000).toISOString(),
              value: `-----BEGIN ML-DSA-65 SIGNATURE-----
MIIFvzCCA6egAwIBAgIUM/WpHYxjDkX/xYrw0+hC7LUjfdswDQYJKoZIhvcNAQELBQAwbzELMAkGA1UEBhMCVk4xDjAMBgNVBAgMBUhhbm9pMQ8wDQYDVQQHDAZCYURpbmgxDDAKBgNVBAoMA0dvdjEPMA0GA1UECwwGUFFDLUNBMSAwHgYDVQQDDBdNb2NrIFBRQyBTaWduYXR1cmUgRGF0YTAeFw0yNTEyMjkwMTQ5MzlaFw0yNjEyMjkwMTQ5MzlaMG8xCzAJBgNVBAYTAlZOMQ4wDAYDVQQIDAVIYW5vaTEPMA0GA1UEBwwGQmFEaW5oMQwwCgYDVQQKDANHb3YxDzANBgNVBAsMBlBRQy1DQTEgMB4GA1UEAwwXTW9jayBQUUMgU2lnbmF0dXJlIERhdGEwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCbQvIznFp0G0ymvQpwGP3V2oCmR3q+XU3fcLWHx3nJJqQ5lT4niv1/psVBn5dEwphFuNfW6BcxULvbLoNLQ6IK/6qf86SaySbYiZeIFtF6aBekPONKsTWRU5zBkQNSrrL1h03agwwaVFmBxgJ9mDlmZ54t1S7LVXt0c/HZ3oWbrjP15DmK24HB/33dCzVt+Gmbo+u4qtaocHvcDTxpqRnKOl9rDNphcS9SK/u6C1/qW8KYG2jvl0aMuyLHKRuoALkYcLrXlxoA+MDxl0dq2gMPimy1vKltoTIpjhxNsV+0+oJxlVab24Yyuv+NA3tL9LVTglxuk6vUbLBGMZiPL4KODp+QqosUWSeJ5zcaztnyJ4Yw6s1WEgKuhBjzPYLsJ0ffSsT1rzrdivBcAWfBRcuTNQ7fZOxIbpeQqBpEi9S1k6ypdb31CgCdeWhH6BdjsWsTlAynAIqpqwFMLUljdJr5S4nsc4yZewhk1Lcz3fMkX3UESQFy1jW7Uxg1M2bRoHHGehH2z7KDcV6fJrL4LBg6A2W7vdYocCVmAqmbo43HbXvwVRG6IkK+AG7hkosPZLbP81fXBlGzMECVYO2atu4FmvPxzmAhfyEzEoH7svO3Wy31711Tb4Xl4SLlqVYUtDIjT7jOssvgxA50y4ABNO4QnWhNRniw3m8hpaVGMRzOxwIDAQABo1MwUTAdBgNVHQ4EFgQU+6WgqjJHs0UOCYotQ2QMXPz8AiswHwYDVR0jBBgwFoAU+6WgqjJHs0UOCYotQ2QMXPz8AiswDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAgEAhDwo0tlNFWo8qWKarAabwq6soklyS6rOYVbfSjZiGC2qQobBb9xi5rbvu7XCxxXo9jmm3Bg7YsDCRFiN6uuCdCPq3mn0wvLWIhsPIJ5mp4KSQCSzaK6do/jDzn4V5bSi5FRxw5gcD2gZNmRvhZX1xs8mF4g76T15R2aP75gMZnoLv2b+oKPSXkubR6iCm3p2FSY7W7kVEC+/oE/KkbFxtuhehtuBqd++nnqb4IhAxLKJw/5myeqGV9u4M0TZ7J/4nsuuKmm38/UFYfjrCNOWzVUDHG9szv/gddTKf6rMY8wrF5FSSPBO3TMfZxub7s5J7/2/d3RESRe36+lI8DasPONgswz7rfr9DvHBgNfpByFN2WQ1twbGEI2UhkUZccKh45kDm2arQCWVADw7Spxi2W+vwXplyfuhdJA5uZao+Yb02FcJm0+4f9I5bdMbxRfMttSrq5CUaByIoy7Sk7SuxTu2SKavgNYk2uEjyCUgsAm4RqcNGG6WDHa6CJp10J/4lvkiD9Ohlhd/pR/m3Epw61UYYVgO5utS8iNP8xhlw8piwesCO2HZT/k/Qz5dNtg9iLgr+W1ozOcMxESacppC+utpQkosctOocxhd81Mraiul5sTt3m79eHDVzKZr/eAGonxDqwQbv4wob69nOwMNZ7DMZWYAAZniY9/d1rJbl94=
-----END ML-DSA-65 SIGNATURE-----`
            }
          ]
        }
      }
    } else {
      verificationResult.value = {
        isValid: false,
        message: 'Invalid signature (Public key mismatch)'
      }
    }
  }, 1200)
}
</script>

<template>
  <div class="page-container">
    <div class="panel">
      <h2>Kiểm tra Chữ ký (Validation Service)</h2>

      <div class="grid">
        <div class="col">
           <label>1. Tài liệu gốc (Original)</label>
           <input type="file" @change="e => file = (e.target as HTMLInputElement).files?.[0] || null" />
        </div>
        <div class="col">
           <label>2. Chữ ký (Base64) - Để trống nếu có trong file</label>
           <textarea v-model="signature" placeholder="Dán base64 nếu file rời..."></textarea>
        </div>
      </div>



      <button @click="handleVerify" :disabled="loading || !file" class="btn-verify">
        {{ loading ? 'Đang kiểm tra...' : 'Xác thực ngay' }}
      </button>

      <div v-if="verificationResult" class="result-box" :class="{ valid: verificationResult.isValid, invalid: !verificationResult.isValid }">
        <div class="result-header">
           <h3 v-if="verificationResult.isValid">✓ Tài liệu Hợp lệ (Valid)</h3>
           <h3 v-else>✗ Tài liệu Không hợp lệ (Invalid)</h3>
           <p v-if="verificationResult.isValid" class="verify-time">Được xác thực lúc: {{ verificationResult.verifiedAt }}</p>
        </div>

        <div class="result-details" v-if="verificationResult.isValid">
          <p><strong>Hash (SHA-256):</strong> {{ verificationResult.details.hash }}</p>
          <p v-if="verificationResult.details.chainStatus"><strong>Chain Status:</strong> <span class="badge success">{{ verificationResult.details.chainStatus }}</span></p>
          
          <!-- TSA Section -->
          <div class="section-block" v-if="verificationResult.details.tsa">
            <div class="section-title" @click="verificationResult.details.tsa.show = !verificationResult.details.tsa.show" style="cursor: pointer; display: flex; justify-content: space-between;">
                <h4>⏱ Chứng nhận Thời gian (TSA)</h4>
                <span>{{ verificationResult.details.tsa.show ? '▼' : '▶' }}</span>
            </div>
            
            <div v-if="verificationResult.details.tsa.show" class="chain-list">
                <div class="chain-item">
                    <div class="cert-icon">⏱</div>
                    <div class="cert-info">
                        <strong>{{ verificationResult.details.tsa.subject.split(',')[0] }}</strong>
                        <div class="cert-meta">Timestamp: {{ verificationResult.details.tsa.timestamp }}</div>
                        <textarea readonly class="cert-pem">{{ verificationResult.details.tsa.value }}</textarea>
                    </div>
                </div>
            </div>
          </div>
          
          <!-- Trust Chain Section -->
          <div class="section-block">
            <div class="section-title" @click="showChain = !showChain" style="cursor: pointer; display: flex; justify-content: space-between;">
                <h4>🔗 Chuỗi Tin Cậy (Trust Chain)</h4>
                <span>{{ showChain ? '▼' : '▶' }}</span>
            </div>
            
            <div v-if="showChain" class="chain-list">
                <div v-for="(cert, cIdx) in verificationResult.details.trustChain" :key="cIdx" class="chain-item">
                    <div class="cert-icon">📜</div>
                    <div class="cert-info">
                        <strong>{{ cert.subject.split(',')[0] }}</strong>
                        <div class="cert-meta">Issuer: {{ cert.issuer.split(',')[0] }}</div>
                        <div class="cert-meta">Fingerprint: {{ cert.fingerprint }}</div>
                        <textarea readonly class="cert-pem">{{ cert.value }}</textarea>
                    </div>
                </div>
            </div>
          </div>

          <div class="signatures-list">
            <h4>Danh sách Chữ ký (Signatures Found):</h4>
            <div v-for="(sig, index) in verificationResult.details.signatures" :key="index" class="sig-item">
              <div class="sig-header">
                <span class="sig-index">#{{ index + 1 }}</span>
                <span class="sig-time">{{ sig.timestamp }}</span>
                <span class="sig-algo badge">{{ sig.algorithm }}</span>
              </div>
              <div class="sig-body">
                <label>Signer:</label> {{ sig.signer }} <br/>
                <label>Signature:</label>
                <textarea readonly>{{ sig.value }}</textarea>
              </div>
            </div>
          </div>
        </div>
        <p v-else>Chi tiết: {{ verificationResult.message }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 2rem; display: flex; justify-content: center; }
.panel { background: white; padding: 2rem; width: 100%; max-width: 800px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
h2 { color: #1a4d8c; border-bottom: 2px solid #eee; padding-bottom: 1rem; margin-bottom: 1.5rem; }
.grid { display: flex; gap: 2rem; margin-bottom: 1.5rem; }
.col { flex: 1; }
.col textarea { width: 100%; height: 100px; padding: 0.5rem; }
.btn-verify { width: 100%; padding: 1rem; background: #1a4d8c; color: white; border: 0; font-size: 1.1rem; cursor: pointer; }
.btn-verify:hover { background: #153d6e; }
.result-box { margin-top: 2rem; padding: 1.5rem; text-align: center; border: 2px solid; border-radius: 8px; }
.result-box.valid { border-color: #28a745; background: #eaffea; color: #155724; }
.result-box.invalid { border-color: #dc3545; background: #ffeaea; color: #721c24; }
.verify-time { font-size: 0.9rem; color: #555; margin-bottom: 1rem; font-style: italic; }

.section-block { margin-top: 1.5rem; border: 1px solid #c3e6cb; background: #fff; border-radius: 4px; overflow: hidden; text-align: left; }
.section-title { background: #d4edda; color: #155724; padding: 0.8rem; font-weight: bold; border-bottom: 1px solid #c3e6cb; }
.section-title h4 { margin: 0; font-size: 1rem; }

.chain-list { padding: 1rem; background: #f9fff9; }
.chain-item { display: flex; gap: 1rem; margin-bottom: 1rem; padding-bottom: 1rem; border-bottom: 1px dashed #ddd; }
.chain-item:last-child { border-bottom: 0; margin-bottom: 0; padding-bottom: 0; }
.cert-icon { font-size: 1.5rem; }
.cert-info { flex: 1; }
.cert-meta { font-size: 0.85rem; color: #666; }
.cert-pem { width: 100%; height: 60px; font-family: monospace; font-size: 0.7rem; color: #888; border: 1px solid #eee; margin-top: 0.5rem; }

.signatures-list { margin-top: 1.5rem; text-align: left; }
.signatures-list h4 { margin-bottom: 1rem; border-bottom: 1px solid #ddd; padding-bottom: 0.5rem; }
.sig-item { background: #fff; border: 1px solid #eee; padding: 1rem; margin-bottom: 1rem; border-radius: 4px; }
.sig-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; background: #f1f3f5; padding: 0.5rem; border-radius: 4px; }
.sig-time { font-size: 0.9rem; color: #666; }

.badge { background: #e3f2fd; color: #0d47a1; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.8rem; }
.badge.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.sig-body textarea { width: 100%; height: 120px; margin-top: 0.3rem; margin-bottom: 0.8rem; padding: 0.5rem; font-family: monospace; font-size: 0.75rem; border: 1px solid #ddd; border-radius: 4px; white-space: pre; }
</style>
