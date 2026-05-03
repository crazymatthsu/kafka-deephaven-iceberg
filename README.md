# kafka-deephaven-iceberg

End-to-end FIX 4.2 OMS simulator: a Java simulator publishes protobuf-encoded
FIX messages to Kafka, a Deephaven server routes them by FIX tag 35 into 5 live
tables, persists snapshots to Iceberg on MinIO, and DuckDB queries the same
Iceberg warehouse.

## Components

```
oms-fix-simulator (Java) ──proto bytes──► Kafka topic 'oms-fix-proto'
                                                │
                                                ▼
                              Deephaven server + deephaven-oms scripts
                              (orders, replaces, cancels, executions, orders_wexecs)
                                                │
                                                ▼
                            Iceberg REST + MinIO bucket 'iceberg-warehouse'
                                                │
                                                ▼
                                        DuckDB ad-hoc queries
```

## Repo layout

| Module | Type | Purpose |
|---|---|---|
| `protobuf/` | Gradle / Java | FIX 4.2 proto schemas + generated Java jar |
| `kafka-server/` | Gradle / compose | KRaft-mode broker; `ProtoProducer` / `ProtoConsumer` byte-array helpers |
| `oms-fix-simulator/` | Gradle / Java app | Generates parent-order lifecycles, publishes to Kafka |
| `deephaven-server/` | Docker / compose | Custom DH image, MinIO, Iceberg REST |
| `deephaven-server/minio/` | compose | Standalone MinIO compose (subset of the parent) |
| `deephaven-oms/` | Python | Mounted into DH server: Kafka consume, route, build `orders_wexecs`, write Iceberg |
| `duckdb/` | compose / SQL | Containerized DuckDB shell + queries against the Iceberg warehouse |

The first three are JVM Gradle subprojects (`./gradlew projects`). The rest are
container-only.

## Demo runbook

```bash
# 0. Copy env defaults
cp .env.example .env

# 1. Build the protobuf jar and publish to maven-local
./gradlew :protobuf:publishToMavenLocal

# 2. Build the custom Deephaven image (downloads pinned extension jars + bakes proto jar)
bash deephaven-server/scripts/build-image.sh

# 3. Bring up the whole stack via the umbrella compose
docker compose up -d            # or: podman compose up -d

# 4. Create the Kafka topic
bash kafka-server/compose/topics.sh

# 5. Publish 500 parent orders (deterministic with --seed)
./gradlew :oms-fix-simulator:run --args="--orders 500 --rate-per-sec 50 --seed 42"

# 6. Open the Deephaven UI and watch the 5 tables tick
open http://localhost:10000/ide/

# 7. Query Iceberg through DuckDB
bash duckdb/scripts/connect.sh -f /queries/latest_order_state.sql

# 8. Tear down
docker compose down
```

## Conventions

- Java 17 + Gradle Kotlin DSL for all JVM modules
- Versions live only in `gradle/libs.versions.toml`
- Protobuf bytes on Kafka (no Schema Registry); a `fix-msg-type` Kafka header
  on every record carries `D`/`G`/`F`/`8` for routing
- Container runtime detection: `docker` if present (Linux), else `podman`
  (Windows). Single script body across all bash helpers.
- Shared compose network `oms-net` declared `external: true` in per-module
  composes; the umbrella creates it once.

## Verification checklist

After running the demo runbook:

1. `./gradlew clean build publishToMavenLocal` is green.
2. The custom DH image builds successfully.
3. All four containers (kafka, minio, iceberg-rest, deephaven) report healthy.
4. `oms-fix-proto` topic exists.
5. The simulator exits 0 after publishing 500 orders.
6. In the DH UI: `orders.size == 500`, `executions.size > 500`,
   `orders_wexecs.size == 500`, and `OrderQty == CumQty + LeavesQty` for every
   `orders_wexecs` row.
7. `mc ls -r minio/iceberg-warehouse/` shows parquet files under
   `oms/orders/`, `oms/replaces/`, `oms/cancels/`, `oms/executions/`,
   `oms/orders_wexecs/`.
8. `duckdb/queries/latest_order_state.sql` returns 500 rows that match step 6
   row-for-row when sorted by `OrderId`.
9. `docker compose down` cleans up; `data/` volumes are gitignored.

## Risks called out in the plan

- **`deephaven-iceberg` API churn** in 0.36.x. If
  `deephaven-oms/app.d/30_iceberg_writers.py` does not match your DH version's
  API, see the fallback note in `deephaven-oms/README.md` (write parquet via
  `parquet.write` and register snapshots with a small `iceberg-core`
  post-process).
- **Protobuf version skew** — DH bundles `protobuf-java`; verify with `jar tf`
  on the image and align `gradle/libs.versions.toml` accordingly.
- **`fix-msg-type` header** is set only by the simulator; the DH UDF falls
  back to parsing the body's `header.msg_type` if the Kafka header is missing.
