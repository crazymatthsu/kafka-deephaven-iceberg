# duckdb

DuckDB containerized client for ad-hoc queries against the Iceberg warehouse on
MinIO.

## Usage

Open an interactive shell with `iceberg` + `httpfs` extensions loaded and the S3
endpoint pointed at MinIO:

```bash
bash duckdb/scripts/connect.sh
```

Run a query file (mounted at `/queries/` inside the container):

```bash
bash duckdb/scripts/connect.sh -f /queries/latest_order_state.sql
```

## Helpers

- `scripts/init.sql` registers an `ice(table_name)` macro that does
  `iceberg_scan('s3://<bucket>/oms/<table_name>')`.
- All five base tables are reachable via `ice('orders')`, `ice('replaces')`,
  `ice('cancels')`, `ice('executions')`, `ice('orders_wexecs')`.

## Query inventory

| File | What it answers |
|---|---|
| `orders_by_account.sql` | All NewOrderSingle rows for one account |
| `orders_by_symbol.sql` | All NewOrderSingle rows for one symbol |
| `order_by_id.sql` | Full lifecycle (D/G/F/8) of one parent OrderId |
| `orders_by_account_symbol_id.sql` | Filter by account + symbol + ClOrdID prefix |
| `latest_order_state.sql` | Recompute `orders_wexecs` server-side from the four base tables |
