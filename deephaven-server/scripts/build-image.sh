#!/usr/bin/env bash
# Build the custom Deephaven image:
#   1. assemble :protobuf jar
#   2. copy it into deephaven-server/extras/
#   3. download pinned third-party extension jars (if missing)
#   4. docker build / podman build
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "${SCRIPT_DIR}")"
REPO_ROOT="$(cd "${MODULE_DIR}/.." && pwd)"
EXTRAS_DIR="${MODULE_DIR}/extras"

DEEPHAVEN_VERSION="${DEEPHAVEN_VERSION:-0.36.1}"
ICEBERG_VERSION="${ICEBERG_VERSION:-1.5.2}"
KAFKA_VERSION="${KAFKA_VERSION:-3.7.1}"
PROTOBUF_VERSION="${PROTOBUF_VERSION:-3.25.5}"

if command -v docker >/dev/null 2>&1; then
  RUNTIME=docker
elif command -v podman >/dev/null 2>&1; then
  RUNTIME=podman
else
  echo "ERROR: neither docker nor podman is installed" >&2
  exit 1
fi

mkdir -p "${EXTRAS_DIR}"

echo "[1/4] Building :protobuf jar"
( cd "${REPO_ROOT}" && ./gradlew :protobuf:jar )
PROTO_JAR="$(ls -1t "${REPO_ROOT}"/protobuf/build/libs/protobuf-*.jar | head -n 1)"
cp "${PROTO_JAR}" "${EXTRAS_DIR}/oms-protobuf-messages.jar"

fetch() {
  local url="$1"
  local dest="$2"
  if [ -f "${dest}" ]; then
    echo "  exists: $(basename "${dest}")"
    return 0
  fi
  echo "  download: $(basename "${dest}")"
  curl -sSfL "${url}" -o "${dest}"
}

echo "[2/4] Fetching extension jars (pinned versions)"
fetch "https://repo1.maven.org/maven2/io/deephaven/deephaven-iceberg/${DEEPHAVEN_VERSION}/deephaven-iceberg-${DEEPHAVEN_VERSION}.jar" \
      "${EXTRAS_DIR}/deephaven-iceberg-${DEEPHAVEN_VERSION}.jar"
fetch "https://repo1.maven.org/maven2/io/deephaven/deephaven-kafka/${DEEPHAVEN_VERSION}/deephaven-kafka-${DEEPHAVEN_VERSION}.jar" \
      "${EXTRAS_DIR}/deephaven-kafka-${DEEPHAVEN_VERSION}.jar"
fetch "https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-aws-bundle/${ICEBERG_VERSION}/iceberg-aws-bundle-${ICEBERG_VERSION}.jar" \
      "${EXTRAS_DIR}/iceberg-aws-bundle-${ICEBERG_VERSION}.jar"
fetch "https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/${KAFKA_VERSION}/kafka-clients-${KAFKA_VERSION}.jar" \
      "${EXTRAS_DIR}/kafka-clients-${KAFKA_VERSION}.jar"
fetch "https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/${PROTOBUF_VERSION}/protobuf-java-${PROTOBUF_VERSION}.jar" \
      "${EXTRAS_DIR}/protobuf-java-${PROTOBUF_VERSION}.jar"

echo "[3/4] Listing extras/"
ls -la "${EXTRAS_DIR}"

echo "[4/4] Building image kafka-deephaven-iceberg/dh-server:local via ${RUNTIME}"
"${RUNTIME}" build -t kafka-deephaven-iceberg/dh-server:local "${MODULE_DIR}"

echo "Done."
