#!/usr/bin/env bash
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
  RUNTIME=podman
else
  echo "ERROR: neither docker nor podman is installed" >&2
  exit 1
fi

"${RUNTIME}" network inspect "${NETWORK}" >/dev/null 2>&1 || "${RUNTIME}" network create "${NETWORK}"

echo "Starting deephaven + minio + iceberg-rest via ${RUNTIME} compose"
"${RUNTIME}" compose -f "${COMPOSE_FILE}" up -d
