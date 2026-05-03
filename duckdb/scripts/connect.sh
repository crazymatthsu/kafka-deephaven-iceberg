#!/usr/bin/env bash
# Open an interactive DuckDB shell with iceberg + httpfs preloaded and S3 settings
# pointed at MinIO on the shared compose network.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../compose/docker-compose.yml"
ENV_FILE="${SCRIPT_DIR}/../../.env"

if [ -f "${ENV_FILE}" ]; then
  set -a; . "${ENV_FILE}"; set +a
fi

if command -v docker >/dev/null 2>&1; then
  RUNTIME=docker
elif command -v podman >/dev/null 2>&1; then
  RUNTIME=podman
else
  echo "ERROR: neither docker nor podman is installed" >&2
  exit 1
fi

# Args after -- are passed through to duckdb (e.g. -f /queries/foo.sql).
"${RUNTIME}" compose -f "${COMPOSE_FILE}" run --rm duckdb \
  duckdb -init /scripts/init.sql "$@"
