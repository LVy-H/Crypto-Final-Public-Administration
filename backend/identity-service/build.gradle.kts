plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common-crypto"))

    // Spring Boot 4 Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // DB
    implementation("org.postgresql:postgresql")
    
    // Kotlin specific
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
