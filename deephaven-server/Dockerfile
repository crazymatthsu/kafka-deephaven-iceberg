# Custom Deephaven server image with Kafka + Iceberg extensions and the OMS protobuf jar
# baked onto the classpath via /apps/extensions.
FROM ghcr.io/deephaven/server:0.36.1

# Drop everything in extras/ on the classpath. build-image.sh populates it before docker build.
COPY extras/ /apps/extensions/

# Mount point for app.d scripts; deephaven-oms volume-mounts here.
RUN mkdir -p /app/app.d
