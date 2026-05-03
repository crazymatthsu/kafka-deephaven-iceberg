plugins {
    java
    `java-library`
    alias(libs.plugins.protobuf)
    `maven-publish`
}

dependencies {
    api(libs.protobuf.java)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "protobuf-messages"
            from(components["java"])
        }
    }
}
