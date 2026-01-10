plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common-crypto"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    // Bouncy Castle is already in common-crypto, but TSA logic might need direct ASN.1 access
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
