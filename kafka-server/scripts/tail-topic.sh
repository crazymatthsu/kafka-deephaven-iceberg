#!/usr/bin/env bash
# Tail a Kafka topic from the broker container. Default topic: oms-fix-proto.
set -euo pipefail

TOPIC="${1:-${KAFKA_TOPIC:-oms-fix-proto}}"

if command -v docker >/dev/null 2>&1; then
  RUNTIME=docker
elif command -v podman >/dev/null 2>&1; then
  RUNTIME=podman
else
  echo "ERROR: neither docker nor podman is installed" >&2
  exit 1
fi

echo "Tailing ${TOPIC} (Ctrl-C to stop)"
"${RUNTIME}" exec -it oms-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic "${TOPIC}" \
  --from-beginning \
  --property print.key=true \
  --property print.headers=true
