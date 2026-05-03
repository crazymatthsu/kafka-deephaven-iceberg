#!/usr/bin/env bash
# Idempotently create the oms-fix-proto topic.
set -euo pipefail

TOPIC="${KAFKA_TOPIC:-oms-fix-proto}"
PARTITIONS="${KAFKA_PARTITIONS:-3}"
RF="${KAFKA_REPLICATION_FACTOR:-1}"

if command -v docker >/dev/null 2>&1; then
  RUNTIME=docker
elif command -v podman >/dev/null 2>&1; then
  RUNTIME=podman
else
  echo "ERROR: neither docker nor podman is installed" >&2
  exit 1
fi

echo "Creating topic ${TOPIC} (partitions=${PARTITIONS}, rf=${RF}) via ${RUNTIME}"
"${RUNTIME}" exec oms-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists \
  --topic "${TOPIC}" \
  --partitions "${PARTITIONS}" \
  --replication-factor "${RF}"

echo
echo "Existing topics:"
"${RUNTIME}" exec oms-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
