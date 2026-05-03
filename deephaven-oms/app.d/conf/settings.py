"""Centralized configuration for deephaven-oms scripts.

All values fall back to environment variables baked into the deephaven container
by deephaven-server/compose/docker-compose.yml.
"""

import os

KAFKA_BOOTSTRAP = os.environ.get("KAFKA_BOOTSTRAP", "kafka:9092")
KAFKA_TOPIC = os.environ.get("KAFKA_TOPIC", "oms-fix-proto")
KAFKA_GROUP_ID = os.environ.get("KAFKA_GROUP_ID", "deephaven-oms")

ICEBERG_CATALOG_URI = os.environ.get("ICEBERG_CATALOG_URI", "http://iceberg-rest:8181")
ICEBERG_WAREHOUSE = os.environ.get("ICEBERG_WAREHOUSE", "s3a://iceberg-warehouse/")
ICEBERG_NAMESPACE = os.environ.get("ICEBERG_NAMESPACE", "oms")

MINIO_ENDPOINT = os.environ.get("MINIO_ENDPOINT", "http://minio:9000")
MINIO_ACCESS_KEY = os.environ.get("MINIO_ROOT_USER", "admin")
MINIO_SECRET_KEY = os.environ.get("MINIO_ROOT_PASSWORD", "adminadmin")

# Iceberg snapshot cadence (seconds). Set to 0 to disable periodic appends.
ICEBERG_APPEND_INTERVAL_SEC = int(os.environ.get("ICEBERG_APPEND_INTERVAL_SEC", "30"))
