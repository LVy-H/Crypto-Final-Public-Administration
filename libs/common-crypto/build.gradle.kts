plugins {
    id("java-library")
}

group = "com.gov.crypto"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven { url = uri("https://ec.europa.eu/cefdigital/artifact/content/repositories/eSig/") }
    maven { url = uri("https://joinup.ec.europa.eu/nexus/content/groups/public/") }
}

// Disable Spring Boot tasks - this is a library, not an app
tasks.findByName("bootJar")?.enabled = false
tasks.findByName("bootBuildImage")?.enabled = false
tasks.named<Jar>("jar") {
    enabled = true
}

dependencies {
    // Bouncy Castle with PQC support (ML-DSA/Dilithium, ML-KEM/Kyber, etc.)
    // Updated to 1.83 for proper ML-DSA (FIPS 204) support
    api("org.bouncycastle:bcprov-jdk18on:1.83")
    api("org.bouncycastle:bcpkix-jdk18on:1.83")
    api("org.bouncycastle:bcutil-jdk18on:1.83")
    
    // Spring Context for @Service annotation
    compileOnly("org.springframework:spring-context:6.2.1")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // SD-DSS Dependencies (EU Digital Signature Service)
    api("eu.europa.ec.joinup.sd-dss:dss-spi:6.3")
    api("eu.europa.ec.joinup.sd-dss:dss-model:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-utils-apache-commons:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-validation-dto:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-crl-parser:6.3")
    api("eu.europa.ec.joinup.sd-dss:dss-validation:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-service:6.3")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}

