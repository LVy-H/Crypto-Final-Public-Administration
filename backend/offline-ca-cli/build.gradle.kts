plugins {
    kotlin("jvm")
    application
}



dependencies {
    implementation(project(":common-crypto"))
    implementation("com.github.ajalt.clikt:clikt:4.2.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
}

application {
    mainClass.set("com.gov.crypto.ca.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
