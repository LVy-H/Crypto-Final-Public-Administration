plugins {
    id("java")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    // No specific crypto lib needed here as we wrap native 'openssl' CLI
    // implementation("commons-io:commons-io:2.15.1") // Good for process IO
}
