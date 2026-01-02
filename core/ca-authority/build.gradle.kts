plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java")
}

repositories {
    mavenCentral()
    maven { url = uri("https://ec.europa.eu/cefdigital/artifact/content/repositories/eSig/") }
    maven { url = uri("https://joinup.ec.europa.eu/nexus/content/groups/public/") }
}

dependencies {
    implementation(project(":libs:common-crypto"))
    implementation(project(":libs:common-model"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aop") // For AspectJ support
    implementation("com.warrenstrange:googleauth:1.5.0")
    runtimeOnly("org.postgresql:postgresql")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")

    // SD-DSS Dependencies (EU Digital Signature Service)
    // SD-DSS Dependencies (EU Digital Signature Service)
    implementation("eu.europa.ec.joinup.sd-dss:dss-spi:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-model:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-utils-apache-commons:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-validation-dto:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-crl-parser:6.3")
    implementation("eu.europa.ec.joinup.sd-dss:dss-validation:6.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
