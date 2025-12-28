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
}

// Disable Spring Boot tasks - this is a library, not an app
tasks.findByName("bootJar")?.enabled = false
tasks.findByName("bootBuildImage")?.enabled = false
tasks.named<Jar>("jar") {
    enabled = true
}

dependencies {
    // Bouncy Castle with PQC support (ML-DSA/Dilithium, ML-KEM/Kyber, etc.)
    api("org.bouncycastle:bcprov-jdk18on:1.78.1")
    api("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    
    // Spring Context for @Service annotation
    compileOnly("org.springframework:spring-context:6.2.1")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}

