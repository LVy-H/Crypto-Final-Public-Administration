plugins {
    id("java")
}

dependencies {
    implementation(project(":libs:common-crypto"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")

    // TOTP support
    implementation("com.warrenstrange:googleauth:1.5.0")
    // Session & Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
