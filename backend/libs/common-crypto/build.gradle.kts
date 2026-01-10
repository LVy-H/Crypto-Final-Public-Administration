plugins {
    kotlin("jvm")
}

dependencies {
    // Bouncy Castle 1.83 (PQC Support)
    api("org.bouncycastle:bcprov-jdk18on:1.83")
    api("org.bouncycastle:bcpkix-jdk18on:1.83")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Spring Support
    compileOnly("org.springframework:spring-context")
}

tasks.jar {
    enabled = true
}

tasks.named("bootJar") {
    enabled = false
}
