rootProject.name = "digital-signature-system"

include("api-gateway")
include("identity-service")
include("ca-authority")
// include("ra-service") // Merged into ca-authority
include("cloud-sign")
include("signature-core")
include("validation-service")
