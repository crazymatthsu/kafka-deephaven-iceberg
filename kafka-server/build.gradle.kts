plugins {
    java
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.kafka.clients)
    api(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
