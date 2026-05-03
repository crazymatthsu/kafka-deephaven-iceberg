# deephaven-server compose

Brings up four containers on `oms-net`:

| service | image | host port | role |
|---|---|---|---|
| `deephaven` | `kafka-deephaven-iceberg/dh-server:local` (built locally) | 10000 | Deephaven server with Python console; mounts `deephaven-oms/app.d` |
| `minio` | `minio/minio:latest` | 9000 / 9001 | S3-compatible object store |
| `minio-mc-init` | `minio/mc:latest` | — | One-shot job that creates the `iceberg-warehouse` bucket |
| `iceberg-rest` | `apache/iceberg-rest-fixture:1.5.2` | 8181 | Iceberg REST catalog backed by MinIO via `s3a://` |

## First-time bring-up

```bash
# 1. Build the custom Deephaven image (requires :protobuf jar already built)
bash deephaven-server/scripts/build-image.sh

# 2. Bring everything up
bash deephaven-server/scripts/start.sh

# 3. Open the Deephaven UI
open http://localhost:10000/ide/
```

Stop with `bash deephaven-server/scripts/stop.sh`.

The shared `oms-net` network is `external: true` so this compose can run alongside
`kafka-server`'s independently. Create the network once via the umbrella compose at the
repo root, or via `docker network create oms-net`.
