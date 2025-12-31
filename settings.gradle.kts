rootProject.name = "crypto-pki"

// Core PKI Services
include(":core:api-gateway")
include(":core:ca-authority")
include(":core:identity-service")
include(":core:org-service")
include(":core:doc-service")
include(":core:signature-core")
include(":core:validation-service")

// Shared Libraries
include(":libs:common-crypto")

// RSSP Services
include(":rssp:cloud-sign")
