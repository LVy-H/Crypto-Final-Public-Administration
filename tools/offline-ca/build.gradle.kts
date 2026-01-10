plugins {
    id("java")
    id("application")
}

group = "com.gov.crypto"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-crypto"))
    implementation("info.picocli:picocli:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")
    
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.gov.crypto.offlineca.OfflineCaTool")
}

tasks.test {
    useJUnitPlatform()
}
