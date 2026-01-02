plugins {
    `java-library`
}

group = "com.gov.crypto"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("org.springframework.security:spring-security-core:6.2.1")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    api("com.fasterxml.jackson.core:jackson-annotations:2.15.3")
}
