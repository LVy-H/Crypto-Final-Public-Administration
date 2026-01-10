plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common-crypto"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Storage & DB
    implementation("org.postgresql:postgresql")
    implementation("io.minio:minio:8.5.10") // Latest Java Client for 2026
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Crypto (CMS/PKCS#7)
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
}
