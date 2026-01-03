rootProject.name = "crypto-pki"

// Core PKI Services
include(":core:api-gateway")
include(":core:ca-authority")
include(":core:identity-service")
include(":core:doc-service")
include(":core:validation-service")

// Shared Libraries
include(":libs:common-crypto")
include(":libs:common-model")

// RSSP Services
include(":rssp:cloud-sign")

// Tools
include(":tools:offline-ca")
