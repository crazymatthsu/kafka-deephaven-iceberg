plugins {
    java
    application
}

dependencies {
    implementation(project(":protobuf"))
    implementation(project(":kafka-server"))
    implementation(libs.kafka.clients)
    implementation(libs.slf4j.api)
    implementation(libs.picocli)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.oms.sim.App")
}
