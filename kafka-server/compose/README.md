# kafka-server

Single-container Kafka broker in KRaft mode (no Zookeeper).

## Listeners

- `INTERNAL://kafka:9092` — for clients on the shared `oms-net` compose network
- `EXTERNAL://localhost:29092` — for clients on the host

## Usage

From the repo root with `.env` in place (copy from `.env.example`):

```bash
bash kafka-server/scripts/start.sh        # bring broker up
bash kafka-server/compose/topics.sh       # create oms-fix-proto
bash kafka-server/scripts/tail-topic.sh   # follow oms-fix-proto
bash kafka-server/scripts/stop.sh         # tear down
```

`start.sh` and `topics.sh` work with both `docker compose` (Linux) and
`podman compose` (Windows); they auto-detect which is installed.
