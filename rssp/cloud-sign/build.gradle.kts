plugins {
    id("java")
}

dependencies {
    implementation(project(":libs:common-crypto"))
    implementation(project(":libs:common-model"))
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

    // Bouncy Castle - inherited from common-crypto (standard version 1.83)
    // implementation("org.bouncycastle:bcprov-jdk18on:1.83") 
    // implementation("org.bouncycastle:bcpkix-jdk18on:1.83")
    // implementation("org.bouncycastle:bcutil-jdk18on:1.83")
    
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
