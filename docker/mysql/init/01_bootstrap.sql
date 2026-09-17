-- Rajneeti MySQL bootstrap.
-- Runs once on the first start of an empty data volume via the MySQL image's
-- docker-entrypoint-initdb.d. Idempotent by design for the default charset.
-- DDL is owned by JPA (ddl-auto=update in dev, validate with migrations in prod).
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;