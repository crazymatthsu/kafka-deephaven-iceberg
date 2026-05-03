-- Configure DuckDB to read Iceberg tables from MinIO over the S3 API.
-- Loaded by connect.sh via duckdb -init duckdb/scripts/init.sql.

INSTALL iceberg;
LOAD iceberg;
INSTALL httpfs;
LOAD httpfs;

-- MinIO uses path-style access and plain HTTP on the compose network.
SET s3_endpoint = 'minio:9000';
SET s3_url_style = 'path';
SET s3_use_ssl = false;
SET s3_access_key_id = getenv('MINIO_ROOT_USER');
SET s3_secret_access_key = getenv('MINIO_ROOT_PASSWORD');

-- Convenience macro: read an Iceberg table from the warehouse by name.
CREATE OR REPLACE MACRO ice(table_name) AS TABLE
  SELECT * FROM iceberg_scan(
    's3://' || getenv('MINIO_BUCKET') || '/oms/' || table_name
  );
