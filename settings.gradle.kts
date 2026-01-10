pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
    }
}

rootProject.name = "crypto-final-public-administration"

include("common-crypto")
project(":common-crypto").projectDir = file("backend/libs/common-crypto")

include("identity-service")
project(":identity-service").projectDir = file("backend/identity-service")

include("pki-service")
project(":pki-service").projectDir = file("backend/pki-service")

include("tsa-service")
project(":tsa-service").projectDir = file("backend/tsa-service")

include("document-service")
project(":document-service").projectDir = file("backend/document-service")

include("api-gateway")
project(":api-gateway").projectDir = file("backend/api-gateway")

include("offline-ca-cli")
project(":offline-ca-cli").projectDir = file("tools/offline-ca")
// project(":libs:common-crypto").projectDir = file("backend/libs/common-crypto")
