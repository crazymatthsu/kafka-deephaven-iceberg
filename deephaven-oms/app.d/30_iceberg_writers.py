"""
Periodically append snapshots of the 5 OMS tables to Iceberg via the
deephaven.experimental.iceberg adapter (REST catalog backed by MinIO over s3a://).

Cadence is configurable via ICEBERG_APPEND_INTERVAL_SEC; set to 0 to disable.
"""

from deephaven.experimental import iceberg
from deephaven.update_graph import auto_locking_ctx

from conf.settings import (
    ICEBERG_APPEND_INTERVAL_SEC,
    ICEBERG_CATALOG_URI,
    ICEBERG_NAMESPACE,
    ICEBERG_WAREHOUSE,
    MINIO_ACCESS_KEY,
    MINIO_ENDPOINT,
    MINIO_SECRET_KEY,
)

_catalog = iceberg.IcebergCatalogAdapter.rest_catalog(
    name="oms",
    uri=ICEBERG_CATALOG_URI,
    warehouse=ICEBERG_WAREHOUSE,
    properties={
        "s3.endpoint": MINIO_ENDPOINT,
        "s3.access-key-id": MINIO_ACCESS_KEY,
        "s3.secret-access-key": MINIO_SECRET_KEY,
        "s3.path-style-access": "true",
    },
)


def _ensure_table(name, dh_table):
    """Create the Iceberg table on first use; partition by day(TransactTime)."""
    qualified = f"{ICEBERG_NAMESPACE}.{name}"
    if not _catalog.has_namespace(ICEBERG_NAMESPACE):
        _catalog.create_namespace(ICEBERG_NAMESPACE)
    if not _catalog.has_table(qualified):
        _catalog.create_table(qualified, dh_table, partition_by=["days(TransactTimeNanos)"])
    return _catalog.load_table(qualified)


# Inputs from prior scripts: orders, replaces, cancels, executions, orders_wexecs
_targets = {
    "orders": orders,
    "replaces": replaces,
    "cancels": cancels,
    "executions": executions,
    "orders_wexecs": orders_wexecs,
}

_writers = {name: _ensure_table(name, t) for name, t in _targets.items()}


def append_all():
    """Snapshot each table once and append to its Iceberg counterpart."""
    with auto_locking_ctx():
        for name, table in _targets.items():
            snapshot = table.snapshot()
            _writers[name].append(snapshot)


if ICEBERG_APPEND_INTERVAL_SEC > 0:
    from deephaven.execution_context import get_exec_ctx
    import threading

    _ctx = get_exec_ctx()

    def _loop():
        import time
        while True:
            time.sleep(ICEBERG_APPEND_INTERVAL_SEC)
            try:
                with _ctx:
                    append_all()
            except Exception as exc:
                print(f"iceberg append failed: {exc}")

    threading.Thread(target=_loop, daemon=True, name="iceberg-appender").start()
