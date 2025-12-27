plugins {
    id("java")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    // No specific crypto lib needed here as we wrap native 'openssl' CLI
    // implementation("commons-io:commons-io:2.15.1") // Good for process IO
}
