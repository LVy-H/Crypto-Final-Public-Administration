plugins {
    id("org.springframework.boot")
}

// Spring Cloud 2025.1.x (Oakwood) is required for Spring Boot 4.0 compatibility
extra["springCloudVersion"] = "2025.1.0"

dependencies {
    // Use the new explicit WebFlux starter for Spring Cloud Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-security")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}
