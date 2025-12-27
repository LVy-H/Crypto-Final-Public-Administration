rootProject.name = "digital-signature-system"

include("common-crypto")
include("api-gateway")
include("identity-service")
include("ca-authority")
// include("ra-service") // Merged into ca-authority
// include("cloud-sign") // Moved to rssp/ as third-party
include("org-service")
include("doc-service")
include("signature-core")
include("validation-service")

