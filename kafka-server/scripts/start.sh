#!/usr/bin/env bash
# Start the Kafka broker (KRaft mode, single container).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../compose/docker-compose.yml"
ENV_FILE="${SCRIPT_DIR}/../../.env"

if [ -f "${ENV_FILE}" ]; then
  set -a; . "${ENV_FILE}"; set +a
fi

NETWORK="${COMPOSE_NETWORK:-oms-net}"

if command -v docker >/dev/null 2>&1; then
  RUNTIME=docker
elif command -v podman >/dev/null 2>&1; then
  RUNTIME="podman"
else
  echo "ERROR: neither docker nor podman is installed" >&2
  exit 1
fi

# Ensure shared network exists (idempotent).
"${RUNTIME}" network inspect "${NETWORK}" >/dev/null 2>&1 || "${RUNTIME}" network create "${NETWORK}"

echo "Starting Kafka via ${RUNTIME} compose"
"${RUNTIME}" compose -f "${COMPOSE_FILE}" up -d

echo "Waiting for Kafka to become healthy..."
for i in $(seq 1 30); do
  if "${RUNTIME}" exec oms-kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
      --bootstrap-server localhost:9092 >/dev/null 2>&1; then
    echo "Kafka is up."
    exit 0
  fi
  sleep 2
done

echo "ERROR: Kafka did not become healthy in time" >&2
exit 1
