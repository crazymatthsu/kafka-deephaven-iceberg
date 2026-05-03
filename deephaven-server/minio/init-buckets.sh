#!/usr/bin/env bash
# One-shot bucket initializer using mc against a running MinIO container.
set -euo pipefail

BUCKET="${MINIO_BUCKET:-iceberg-warehouse}"
ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
USER="${MINIO_ROOT_USER:-admin}"
PASS="${MINIO_ROOT_PASSWORD:-adminadmin}"

if command -v docker >/dev/null 2>&1; then
  RUNTIME=docker
elif command -v podman >/dev/null 2>&1; then
  RUNTIME=podman
else
  echo "ERROR: neither docker nor podman is installed" >&2
  exit 1
fi

"${RUNTIME}" run --rm --network "${COMPOSE_NETWORK:-oms-net}" minio/mc:latest \
  /bin/sh -c "mc alias set local ${ENDPOINT} ${USER} ${PASS} && mc mb --ignore-existing local/${BUCKET}"
