plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kafka-deephaven-iceberg"

include(
    ":protobuf",
    ":kafka-server",
    ":oms-fix-simulator",
)
