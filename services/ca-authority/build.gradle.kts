plugins {
    id("java")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    // implementation("org.bouncycastle:bcpkix-jdk18on:1.78") // Keep BouncyCastle as backup or for non-PQC ops
}
