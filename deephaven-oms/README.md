# deephaven-oms

Python scripts that run inside the Deephaven server. They are mounted at
`/app/app.d` in the `deephaven` container by `deephaven-server/compose/docker-compose.yml`.

## Pipeline

```
Kafka oms-fix-proto
   │
   ▼
00_kafka_source.py     consume bytes; UDF dispatch on fix-msg-type header
   │   raw_msgs (one ticking append-only table)
   ▼
10_route_by_msg_type.py  orders / replaces / cancels / executions / latest_exec_by_order
   │
   ▼
20_build_orders_wexecs.py  joined view keyed by OrderId
   │
   ▼
30_iceberg_writers.py     periodic IcebergTableWriter.append per table
                          → s3a://iceberg-warehouse/<namespace>/<table>/
```

## Why not `rollup` for joining?

`Table.rollup` aggregates one table hierarchically (e.g.
`executions.rollup(["OrderId"], aggs=[agg.sum_("CumQty")])` to compute per-order fill
totals). It is **not** a multi-table join — joining `orders/replaces/cancels/executions`
by `OrderId` and `ClOrdId` is what `natural_join` and `last_by` are for.

The `orders_wexecs` table in `20_build_orders_wexecs.py` uses `natural_join` + `last_by`
to keep one current row per parent `OrderId`.

## Iceberg API caveat (from the plan)

The `deephaven-iceberg` extension API has had renames across 0.36.x. If
`30_iceberg_writers.py` fails to import or call:

- `IcebergCatalogAdapter.rest_catalog` — check whether your DH version exposes
  `IcebergToolsS3.createAdapter` instead.
- `partition_by=` kwarg — some versions take a `partition_spec` object.
- `append(snapshot)` — alternative shape is `IcebergTableWriter.append(table)` or
  `add_snapshot(...)`.

If the writer API does not match what you're seeing, the documented fallback is to
write parquet via `parquet.write` to a local volume and register it as Iceberg
snapshots with a small `iceberg-core` post-processor.
